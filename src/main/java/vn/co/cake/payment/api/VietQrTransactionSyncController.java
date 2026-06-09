package vn.co.cake.payment.api;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.co.cake.exception.CommonServletException;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.dto.VietQrTransactionSyncRequest;
import lombok.extern.slf4j.Slf4j;
import vn.co.cake.payment.support.PaymentCheckoutFlowLog;
import vn.co.cake.payment.service.VietQrPartnerTokenService;
import vn.co.cake.payment.service.VietQrWebhookService;

@Slf4j
@RestController
@RequestMapping("/vqr/bank/api")
public class VietQrTransactionSyncController {

    private final VietQrPartnerTokenService vietQrPartnerTokenService;
    private final VietQrWebhookService vietQrWebhookService;
    private final ObjectMapper objectMapper;

    public VietQrTransactionSyncController(VietQrPartnerTokenService vietQrPartnerTokenService,
                                           VietQrWebhookService vietQrWebhookService,
                                           ObjectMapper objectMapper) {
        this.vietQrPartnerTokenService = vietQrPartnerTokenService;
        this.vietQrWebhookService = vietQrWebhookService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/transaction-sync")
    public ResponseEntity<Map<String, Object>> transactionSync(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody VietQrTransactionSyncRequest payload) {
        String traceId = payload != null ? payload.getOrderId() : null;
        PaymentCheckoutFlowLog.step(traceId, 1,
                "Nhận callback VietQR POST /transaction-sync — transType=%s, amount=%s",
                payload != null ? payload.getTransType() : null,
                payload != null ? payload.getAmount() : null);

        Map<String, Object> body = new HashMap<>();
        if (!vietQrPartnerTokenService.isValidBearerToken(authorization)) {
            PaymentCheckoutFlowLog.step(traceId, 1, "LỖI: Bearer token không hợp lệ — từ chối webhook");
            body.put("status", "FAILED");
            body.put("message", "INVALID_TOKEN");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }
        try {
            String requestJson = objectMapper.writeValueAsString(payload);
            vietQrWebhookService.confirmPayment(payload, requestJson);
            PaymentCheckoutFlowLog.step(traceId, 1, "Webhook xử lý thành công — trả SUCCESS cho VietQR");
            body.put("status", "SUCCESS");
            body.put("message", "");
            return ResponseEntity.ok(body);
        } catch (CommonServletException ex) {
            PaymentCheckoutFlowLog.step(traceId, 1, "Webhook thất bại (business): %s", ex.getMessage());
            body.put("status", "FAILED");
            body.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
        } catch (Exception ex) {
            log.error("VietQR transaction-sync error, orderId={}", traceId, ex);
            PaymentCheckoutFlowLog.step(traceId, 1, "Webhook thất bại (system): %s", ex.getMessage());
            body.put("status", "FAILED");
            body.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
    }
}
