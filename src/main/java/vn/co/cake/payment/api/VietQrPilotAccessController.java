package vn.co.cake.payment.api;

import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.co.cake.controller.BaseController;
import vn.co.cake.payment.dto.VietQrAccessStatusResponse;
import vn.co.cake.payment.dto.VietQrPilotUnlockRequest;
import vn.co.cake.payment.service.VietQrPilotAccessService;
import vn.co.cake.response.CommonResponse;
import vn.co.cake.security.user.UserLoginInfo;

import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/payment/vietqr-access")
public class VietQrPilotAccessController extends BaseController {

  private final VietQrPilotAccessService vietQrPilotAccessService;

  public VietQrPilotAccessController(VietQrPilotAccessService vietQrPilotAccessService) {
    this.vietQrPilotAccessService = vietQrPilotAccessService;
  }

  @GetMapping
  public ResponseEntity<VietQrAccessStatusResponse> status(HttpSession session) {
    UserLoginInfo loginInfo = getLoginInfo(session);
    Long accountId = loginInfo != null ? loginInfo.getId() : null;
    return ResponseEntity.ok(vietQrPilotAccessService.buildAccessStatus(session, accountId));
  }

  @PostMapping("/unlock")
  public ResponseEntity<?> unlock(@RequestBody VietQrPilotUnlockRequest request, HttpSession session) {
    UserLoginInfo loginInfo = getLoginInfo(session);
    if (Objects.isNull(loginInfo)) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN).body(CommonResponse.builder()
          .result("FORBIDDEN")
          .message("Vui lòng đăng nhập")
          .build());
    }
    boolean ok = vietQrPilotAccessService.unlockPilot(session, loginInfo.getId(),
        request != null ? request.getPassword() : null);
    if (!ok) {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(CommonResponse.builder()
          .result("BAD_REQUEST")
          .message(VietQrPilotAccessService.PILOT_MESSAGE)
          .build());
    }
    return ResponseEntity.ok(vietQrPilotAccessService.buildAccessStatus(session, loginInfo.getId()));
  }
}
