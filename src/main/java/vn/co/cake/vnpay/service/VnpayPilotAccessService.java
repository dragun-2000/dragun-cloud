package vn.co.cake.vnpay.service;

import java.util.Date;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.co.cake.exception.CommonServletException;
import vn.co.cake.vnpay.VnpayConstants;
import vn.co.cake.vnpay.dto.VnpayAccessStatusResponse;
import vn.co.cake.vnpay.entity.VnpaySettings;
import vn.co.cake.vnpay.repository.VnpaySettingsRepository;

@Service
public class VnpayPilotAccessService {

    public static final String PILOT_MESSAGE =
            "Tính năng thanh toán VNPAY đang pilot. Những khách hàng được admin cung cấp password mới được sử dụng. "
                    + "Xin mọi người vui lòng chờ tính năng mở trong thời gian tới.";

    private final VnpaySettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;

    public VnpayPilotAccessService(VnpaySettingsRepository settingsRepository,
                                   @Qualifier("userPasswordEncoder") PasswordEncoder passwordEncoder) {
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public VnpaySettings getSettings() {
        return settingsRepository.findById(VnpaySettings.SINGLETON_ID)
                .orElseGet(this::createDefaultSettings);
    }

    @Transactional
    public VnpaySettings saveSettings(boolean enabled, boolean visibleToAll, String plainPilotPassword,
                                      String updater) {
        VnpaySettings settings = getSettings();
        settings.setEnabled(enabled);
        settings.setVisibleToAll(visibleToAll);
        if (StringUtils.isNotBlank(plainPilotPassword)) {
            settings.setPilotPasswordHash(passwordEncoder.encode(plainPilotPassword.trim()));
        } else if (enabled && !visibleToAll && StringUtils.isBlank(settings.getPilotPasswordHash())) {
            throw new IllegalArgumentException("Chế độ pilot VNPAY cần đặt mật khẩu truy cập.");
        }
        settings.setUpdated(new Date());
        settings.setUpdater(updater);
        return settingsRepository.save(settings);
    }

    public VnpayAccessStatusResponse buildAccessStatus(HttpSession session, Long accountId) {
        VnpaySettings settings = getSettings();
        boolean visibleToAll = settings.isVisibleToAll();
        boolean pilotMode = !visibleToAll;
        boolean unlocked = hasPilotUnlock(session, accountId);
        boolean canUse = visibleToAll || unlocked;
        return VnpayAccessStatusResponse.builder()
                .visibleToAll(visibleToAll)
                .pilotMode(pilotMode)
                .unlocked(unlocked)
                .canUseVnpay(canUse)
                .pilotMessage(pilotMode && !canUse ? PILOT_MESSAGE : null)
                .build();
    }

    public void assertCanUseVnpay(HttpSession session, Long accountId) throws CommonServletException {
        if (buildAccessStatus(session, accountId).isCanUseVnpay()) {
            return;
        }
        throw new CommonServletException(PILOT_MESSAGE);
    }

    public boolean unlockPilot(HttpSession session, Long accountId, String plainPassword) {
        if (accountId == null) {
            return false;
        }
        VnpaySettings settings = getSettings();
        if (settings.isVisibleToAll()) {
            markUnlocked(session, accountId);
            return true;
        }
        if (StringUtils.isBlank(plainPassword) || StringUtils.isBlank(settings.getPilotPasswordHash())) {
            return false;
        }
        if (!passwordEncoder.matches(plainPassword.trim(), settings.getPilotPasswordHash())) {
            return false;
        }
        markUnlocked(session, accountId);
        return true;
    }

    public void clearPilotUnlock(HttpSession session) {
        if (session != null) {
            session.removeAttribute(VnpayConstants.SESSION_VNPAY_PILOT_ACCOUNT_ID);
        }
    }

    private boolean hasPilotUnlock(HttpSession session, Long accountId) {
        if (session == null || accountId == null) {
            return false;
        }
        Object value = session.getAttribute(VnpayConstants.SESSION_VNPAY_PILOT_ACCOUNT_ID);
        if (value == null) {
            return false;
        }
        if (value instanceof Long) {
            return accountId.equals(value);
        }
        return accountId.toString().equals(String.valueOf(value));
    }

    private void markUnlocked(HttpSession session, Long accountId) {
        session.setAttribute(VnpayConstants.SESSION_VNPAY_PILOT_ACCOUNT_ID, accountId);
    }

    private VnpaySettings createDefaultSettings() {
        VnpaySettings settings = new VnpaySettings();
        settings.setId(VnpaySettings.SINGLETON_ID);
        settings.setEnabled(true);
        settings.setVisibleToAll(false);
        return settingsRepository.save(settings);
    }
}
