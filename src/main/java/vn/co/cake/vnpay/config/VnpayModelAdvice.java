package vn.co.cake.vnpay.config;

import javax.servlet.http.HttpSession;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.common.BaseConst;
import vn.co.cake.controller.store.CartController;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.vnpay.dto.VnpayAccessStatusResponse;
import vn.co.cake.vnpay.service.VnpayPilotAccessService;
import vn.co.cake.vnpay.service.VnpaySettingsService;

/** Đưa cờ {@code vnpayEnabled} / {@code vnpayAccess} vào trang thanh toán mà không sửa CartController. */
@Slf4j
@ControllerAdvice(assignableTypes = CartController.class)
public class VnpayModelAdvice {

    private final VnpayProperties properties;
    private final VnpaySettingsService settingsService;
    private final VnpayPilotAccessService pilotAccessService;

    public VnpayModelAdvice(VnpayProperties properties,
                            VnpaySettingsService settingsService,
                            VnpayPilotAccessService pilotAccessService) {
        this.properties = properties;
        this.settingsService = settingsService;
        this.pilotAccessService = pilotAccessService;
    }

    @ModelAttribute
    public void addVnpayFlag(Model model, HttpSession session) {
        boolean adminEnabled = true;
        try {
            adminEnabled = settingsService.isFeatureEnabled();
        } catch (Exception ex) {
            log.warn("VNPAY settings lookup failed — treat as enabled: {}", ex.getMessage());
        }
        boolean vnpayEnabled = properties.isUsable() && adminEnabled;
        model.addAttribute("vnpayEnabled", vnpayEnabled);

        VnpayAccessStatusResponse access = null;
        try {
            Long accountId = null;
            Object login = session != null ? session.getAttribute(BaseConst.USER_SESSION) : null;
            if (login instanceof UserLoginInfo) {
                accountId = ((UserLoginInfo) login).getId();
            }
            access = pilotAccessService.buildAccessStatus(session, accountId);
        } catch (Exception ex) {
            log.warn("VNPAY pilot access lookup failed: {}", ex.getMessage());
            access = VnpayAccessStatusResponse.builder()
                    .visibleToAll(false)
                    .pilotMode(true)
                    .unlocked(false)
                    .canUseVnpay(false)
                    .pilotMessage(VnpayPilotAccessService.PILOT_MESSAGE)
                    .build();
        }
        model.addAttribute("vnpayAccess", access);
        model.addAttribute("vnpayPilotMode", access != null && access.isPilotMode());
    }
}
