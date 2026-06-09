package vn.co.cake.payment.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.service.PaymentTransactionLogService;
import vn.co.cake.payment.service.VietQrPartnerTokenService;

@RestController
@RequestMapping("/vqr/api")
public class VietQrPartnerTokenController {

    private final VietQrPartnerTokenService vietQrPartnerTokenService;
    private final PaymentTransactionLogService paymentTransactionLogService;

    public VietQrPartnerTokenController(VietQrPartnerTokenService vietQrPartnerTokenService,
                                        PaymentTransactionLogService paymentTransactionLogService) {
        this.vietQrPartnerTokenService = vietQrPartnerTokenService;
        this.paymentTransactionLogService = paymentTransactionLogService;
    }

    @PostMapping("/token_generate")
    public ResponseEntity<Map<String, Object>> generateToken(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!vietQrPartnerTokenService.isValidBasicCredentials(authorization)) {
            paymentTransactionLogService.logFailure(PaymentConstants.METHOD_VIETQR,
                    PaymentConstants.EVENT_VIETQR_PARTNER_GET_TOKEN, null, null, null, null,
                    null, "UNAUTHORIZED", "Invalid partner credentials", null, null, 401, null);
            Map<String, Object> body = new HashMap<>();
            body.put("status", "FAILED");
            body.put("message", "UNAUTHORIZED");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        }
        String token = vietQrPartnerTokenService.generateBearerToken();
        paymentTransactionLogService.logSuccess(PaymentConstants.METHOD_VIETQR,
                PaymentConstants.EVENT_VIETQR_PARTNER_GET_TOKEN, null, null, null, null,
                null, null, null, null, PaymentConstants.LOG_STATUS_SUCCESS, 200, null);

        Map<String, Object> body = new HashMap<>();
        body.put("access_token", token);
        body.put("token_type", PaymentConstants.TOKEN_TYPE_BEARER);
        body.put("expires_in", PaymentConstants.VIETQR_TOKEN_EXPIRES_SECONDS);
        return ResponseEntity.ok(body);
    }
}
