package vn.co.cake.payment.service;

import java.util.Date;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.co.cake.exception.CommonServletException;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.dto.VietQrAccessStatusResponse;
import vn.co.cake.payment.entity.VietQrPilotSettings;
import vn.co.cake.payment.repository.VietQrPilotSettingsRepository;

@Service
public class VietQrPilotAccessService {

  public static final String PILOT_MESSAGE =
      "Tính năng này đang pilot. Những khách hàng được admin cung cấp password mới được sử dụng. "
          + "Xin mọi người vui lòng chờ tính năng mở trong thời gian tới.";

  private final VietQrPilotSettingsRepository settingsRepository;
  private final PasswordEncoder passwordEncoder;

  public VietQrPilotAccessService(VietQrPilotSettingsRepository settingsRepository,
                                  @Qualifier("userPasswordEncoder") PasswordEncoder passwordEncoder) {
    this.settingsRepository = settingsRepository;
    this.passwordEncoder = passwordEncoder;
  }

  @Transactional(readOnly = true)
  public VietQrPilotSettings getSettings() {
    return settingsRepository.findById(VietQrPilotSettings.SINGLETON_ID)
        .orElseGet(this::createDefaultSettings);
  }

  @Transactional
  public VietQrPilotSettings saveSettings(boolean visibleToAll, String plainPilotPassword, String updater) {
    VietQrPilotSettings settings = getSettings();
    settings.setVisibleToAll(visibleToAll);
    if (StringUtils.isNotBlank(plainPilotPassword)) {
      settings.setPilotPasswordHash(passwordEncoder.encode(plainPilotPassword.trim()));
    } else if (!visibleToAll && StringUtils.isBlank(settings.getPilotPasswordHash())) {
      throw new IllegalArgumentException("Chế độ pilot cần đặt mật khẩu truy cập.");
    }
    settings.setUpdated(new Date());
    settings.setUpdater(updater);
    return settingsRepository.save(settings);
  }

  public VietQrAccessStatusResponse buildAccessStatus(HttpSession session, Long accountId) {
    VietQrPilotSettings settings = getSettings();
    boolean visibleToAll = settings.isVisibleToAll();
    boolean pilotMode = !visibleToAll;
    boolean unlocked = hasPilotUnlock(session, accountId);
    boolean canUse = visibleToAll || unlocked;
    return VietQrAccessStatusResponse.builder()
        .visibleToAll(visibleToAll)
        .pilotMode(pilotMode)
        .unlocked(unlocked)
        .canUseVietQr(canUse)
        .pilotMessage(pilotMode && !canUse ? PILOT_MESSAGE : null)
        .build();
  }

  public void assertCanUseVietQr(HttpSession session, Long accountId) throws CommonServletException {
    if (buildAccessStatus(session, accountId).isCanUseVietQr()) {
      return;
    }
    throw new CommonServletException(PILOT_MESSAGE);
  }

  public boolean unlockPilot(HttpSession session, Long accountId, String plainPassword) {
    if (accountId == null) {
      return false;
    }
    VietQrPilotSettings settings = getSettings();
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
      session.removeAttribute(PaymentConstants.SESSION_VIETQR_PILOT_ACCOUNT_ID);
    }
  }

  private boolean hasPilotUnlock(HttpSession session, Long accountId) {
    if (session == null || accountId == null) {
      return false;
    }
    Object value = session.getAttribute(PaymentConstants.SESSION_VIETQR_PILOT_ACCOUNT_ID);
    if (value == null) {
      return false;
    }
    if (value instanceof Long) {
      return accountId.equals(value);
    }
    return accountId.toString().equals(String.valueOf(value));
  }

  private void markUnlocked(HttpSession session, Long accountId) {
    session.setAttribute(PaymentConstants.SESSION_VIETQR_PILOT_ACCOUNT_ID, accountId);
  }

  private VietQrPilotSettings createDefaultSettings() {
    VietQrPilotSettings settings = new VietQrPilotSettings();
    settings.setId(VietQrPilotSettings.SINGLETON_ID);
    settings.setVisibleToAll(true);
    return settingsRepository.save(settings);
  }
}
