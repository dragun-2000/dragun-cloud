package vn.co.cake.payment.vietqr;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import vn.co.cake.exception.CommonServletException;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.config.VietQrProperties;
import vn.co.cake.payment.dto.VietQrGenerateResponse;
import vn.co.cake.payment.dto.VietQrTokenResponse;
import vn.co.cake.payment.service.PaymentTransactionLogService;

@Component
public class VietQrApiClient {

    private static final String PATH_TOKEN = "/vqr/api/token_generate";
    private static final String PATH_GENERATE_QR = "/vqr/api/qr/generate-customer";

    private final VietQrProperties vietQrProperties;
    private final PaymentTransactionLogService paymentTransactionLogService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private String cachedToken;
    private long tokenExpiresAtMs;

    public VietQrApiClient(VietQrProperties vietQrProperties,
                           PaymentTransactionLogService paymentTransactionLogService,
                           @Qualifier("vietQrRestTemplate") RestTemplate restTemplate,
                           ObjectMapper objectMapper) {
        this.vietQrProperties = vietQrProperties;
        this.paymentTransactionLogService = paymentTransactionLogService;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public VietQrGenerateResponse generateCustomerQr(String vietqrOrderId, BigDecimal amount, String content,
                                                     String paymentMethodForLog) throws Exception {
        String token = getAccessToken(paymentMethodForLog);
        String url = vietQrProperties.getApi().getBaseUrl() + PATH_GENERATE_QR;

        Map<String, Object> body = new HashMap<>();
        body.put("bankCode", vietQrProperties.getBank().getCode());
        body.put("bankAccount", vietQrProperties.getBank().getAccount());
        body.put("userBankName", vietQrProperties.getBank().getHolder());
        body.put("content", content);
        body.put("qrType", PaymentConstants.VIETQR_QR_TYPE_DYNAMIC);
        body.put("amount", amount.longValue());
        body.put("orderId", vietqrOrderId);
        body.put("transType", PaymentConstants.VIETQR_TRANS_TYPE_CREDIT);
        // Thời hạn quét/thanh toán QR trên VietQR (phút).
        int expireMinutes = Math.max(vietQrProperties.getCheckout().getExpireMinutes(), 1);
        body.put("timeOut", expireMinutes);

        String requestJson = objectMapper.writeValueAsString(body);
        long start = System.currentTimeMillis();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<VietQrGenerateResponse> response =
                    restTemplate.postForEntity(url, entity, VietQrGenerateResponse.class);
            long duration = System.currentTimeMillis() - start;
            String responseJson = objectMapper.writeValueAsString(response.getBody());
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null
                    && response.getBody().getQrCode() != null) {
                paymentTransactionLogService.logInfo(paymentMethodForLog, PaymentConstants.EVENT_VIETQR_GENERATE_QR,
                        vietqrOrderId, null, amount, requestJson, responseJson,
                        response.getStatusCodeValue(), duration);
                return response.getBody();
            }
            paymentTransactionLogService.logFailure(paymentMethodForLog, PaymentConstants.EVENT_VIETQR_GENERATE_QR,
                    vietqrOrderId, null, null, null, amount, "GENERATE_QR_FAILED",
                    response.getBody() != null ? response.getBody().getMessage() : "Empty response",
                    requestJson, responseJson, response.getStatusCodeValue(), duration);
            throw new IllegalStateException("VietQR generate QR failed");
        } catch (HttpClientErrorException ex) {
            long duration = System.currentTimeMillis() - start;
            String errorDetail = formatHttpError(ex);
            paymentTransactionLogService.logFailure(paymentMethodForLog, PaymentConstants.EVENT_VIETQR_GENERATE_QR,
                    vietqrOrderId, null, null, null, amount, "GENERATE_QR_HTTP_" + ex.getStatusCode().value(),
                    errorDetail, requestJson, ex.getResponseBodyAsString(), ex.getRawStatusCode(), duration);
            throw new CommonServletException("VietQR tạo mã QR thất bại: " + errorDetail);
        } catch (RestClientException ex) {
            long duration = System.currentTimeMillis() - start;
            paymentTransactionLogService.logFailure(paymentMethodForLog, PaymentConstants.EVENT_VIETQR_GENERATE_QR,
                    vietqrOrderId, null, null, null, amount, "GENERATE_QR_ERROR", ex.getMessage(),
                    requestJson, null, null, duration);
            throw new CommonServletException("Không kết nối được VietQR: " + ex.getMessage());
        }
    }

    private synchronized String getAccessToken(String paymentMethodForLog) throws Exception {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpiresAtMs) {
            return cachedToken;
        }
        String url = vietQrProperties.getApi().getBaseUrl() + PATH_TOKEN;
        String credentials = String.format(Locale.ROOT, "%s:%s",
                vietQrProperties.getClient().getUsername(),
                vietQrProperties.getClient().getPassword());
        String basic = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Basic " + basic);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        long start = System.currentTimeMillis();
        try {
            ResponseEntity<VietQrTokenResponse> response =
                    restTemplate.postForEntity(url, entity, VietQrTokenResponse.class);
            long duration = System.currentTimeMillis() - start;
            VietQrTokenResponse body = response.getBody();
            String responseJson = objectMapper.writeValueAsString(body);
            if (response.getStatusCode() == HttpStatus.OK && body != null && body.getAccessToken() != null) {
                cachedToken = body.getAccessToken();
                int expiresIn = body.getExpiresIn() != null ? body.getExpiresIn() : PaymentConstants.VIETQR_TOKEN_EXPIRES_SECONDS;
                tokenExpiresAtMs = System.currentTimeMillis() + (expiresIn - 30L) * 1000L;
                paymentTransactionLogService.logSuccess(paymentMethodForLog, PaymentConstants.EVENT_VIETQR_GET_TOKEN,
                        null, null, null, null, null, null, null,
                        null, responseJson, response.getStatusCodeValue(), duration);
                return cachedToken;
            }
            paymentTransactionLogService.logFailure(paymentMethodForLog, PaymentConstants.EVENT_VIETQR_GET_TOKEN,
                    null, null, null, null, null, "TOKEN_FAILED",
                    body != null ? body.getMessage() : "Empty token response",
                    null, responseJson, response.getStatusCodeValue(), duration);
            throw new IllegalStateException("VietQR get token failed");
        } catch (HttpClientErrorException ex) {
            long duration = System.currentTimeMillis() - start;
            String errorDetail = formatHttpError(ex);
            paymentTransactionLogService.logFailure(paymentMethodForLog, PaymentConstants.EVENT_VIETQR_GET_TOKEN,
                    null, null, null, null, null, "TOKEN_HTTP_" + ex.getStatusCode().value(),
                    errorDetail, null, ex.getResponseBodyAsString(), ex.getRawStatusCode(), duration);
            throw new CommonServletException(
                    "VietQR từ chối đăng nhập (" + ex.getStatusCode().value() + "). "
                            + "Kiểm tra vietqr.client.username / vietqr.client.password. Chi tiết: " + errorDetail);
        } catch (RestClientException ex) {
            long duration = System.currentTimeMillis() - start;
            paymentTransactionLogService.logFailure(paymentMethodForLog, PaymentConstants.EVENT_VIETQR_GET_TOKEN,
                    null, null, null, null, null, "TOKEN_ERROR", ex.getMessage(),
                    null, null, null, duration);
            throw new CommonServletException("Không kết nối được máy chủ VietQR: " + ex.getMessage());
        }
    }

    private static String formatHttpError(HttpClientErrorException ex) {
        String body = ex.getResponseBodyAsString();
        if (body != null && !body.isBlank()) {
            return ex.getStatusCode() + " — " + body;
        }
        return ex.getMessage() != null ? ex.getMessage() : String.valueOf(ex.getStatusCode().value());
    }
}
