package vn.co.cake.vnpay.api;

import java.util.Objects;

import javax.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.co.cake.controller.BaseController;
import vn.co.cake.response.CommonResponse;
import vn.co.cake.security.user.UserLoginInfo;
import vn.co.cake.vnpay.dto.VnpayAccessStatusResponse;
import vn.co.cake.vnpay.dto.VnpayPilotUnlockRequest;
import vn.co.cake.vnpay.service.VnpayPilotAccessService;

@RestController
@RequestMapping("/api/payment/vnpay-access")
public class VnpayPilotAccessController extends BaseController {

    private final VnpayPilotAccessService vnpayPilotAccessService;

    public VnpayPilotAccessController(VnpayPilotAccessService vnpayPilotAccessService) {
        this.vnpayPilotAccessService = vnpayPilotAccessService;
    }

    @GetMapping
    public ResponseEntity<VnpayAccessStatusResponse> status(HttpSession session) {
        UserLoginInfo loginInfo = getLoginInfo(session);
        Long accountId = loginInfo != null ? loginInfo.getId() : null;
        return ResponseEntity.ok(vnpayPilotAccessService.buildAccessStatus(session, accountId));
    }

    @PostMapping("/unlock")
    public ResponseEntity<?> unlock(@RequestBody VnpayPilotUnlockRequest request, HttpSession session) {
        UserLoginInfo loginInfo = getLoginInfo(session);
        if (Objects.isNull(loginInfo)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CommonResponse.builder()
                    .result("FORBIDDEN")
                    .message("Vui lòng đăng nhập")
                    .build());
        }
        boolean ok = vnpayPilotAccessService.unlockPilot(session, loginInfo.getId(),
                request != null ? request.getPassword() : null);
        if (!ok) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.builder()
                    .result("BAD_REQUEST")
                    .message(VnpayPilotAccessService.PILOT_MESSAGE)
                    .build());
        }
        return ResponseEntity.ok(vnpayPilotAccessService.buildAccessStatus(session, loginInfo.getId()));
    }
}
