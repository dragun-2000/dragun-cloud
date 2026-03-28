package vn.co.cake.helper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import vn.co.cake.utils.PhoneValidator;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Gửi SMS qua Tingting SMS API (POST JSON, header {@code apikey}).
 * <p>
 * Khi API trả {@code status: success}, đó là tin đã được Tingting <strong>chấp nhận</strong> (tính phí / vào hàng đợi),
 * không đồng nghĩa tin đã <strong>phát tới máy</strong> người nhận. Trạng thái gửi thực tế cần xem trên dashboard
 * Tingting hoặc webhook delivery (nếu cấu hình).
 *
 * @see <a href="https://tingting.im/tingting-sms/">Tingting SMS API</a>
 */
@Slf4j
@Service
public class SmsService {

    private static final String JSON_STATUS = "status";
    private static final String JSON_TRAN_ID = "tranId";
    /** Số lượng SMS (theo độ dài nội dung) — trường trong response success. */
    private static final String JSON_SMS = "sms";
    /** Chi phí — trường trong response success. */
    private static final String JSON_COST = "cost";
    private static final String JSON_CODE = "code";
    private static final String JSON_MESSAGE = "message";
    private static final String STATUS_SUCCESS = "success";
    private static final String STATUS_ERROR = "error";
    private static final String HEADER_API_KEY = "apikey";

    /** Theo tài liệu Tingting: brandname tối đa 11 ký tự. */
    private static final int SENDER_MAX_LENGTH = 11;

    /**
     * Khớp template Tingting đã duyệt; placeholder {@code {D,6}} được thay bằng mã OTP khi gửi API.
     */
    private static final String OTP_MESSAGE_TEMPLATE =
            "Quy Khach dang dang ky tai khoan de base. Ma OTP cua ban tai debase.vn la%s";

    /** Tránh lỗi String.format khi mã OTP chứa ký tự % */
    private static final String OTP_PATTERN = "\\d{4,10}";

    private static final int LOG_BODY_MAX = 500;

    @Value("${tingting.api.key:}")
    private String apiKey;

    @Value("${tingting.api.sender:}")
    private String sender;

    @Value("${tingting.api.url:https://v1.tingting.im/api/sms}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SmsService() {
        this.objectMapper = new ObjectMapper();
        this.restTemplate = new RestTemplate();
        // Không ném exception khi HTTP 4xx/5xx — đọc body JSON lỗi của Tingting để log đầy đủ
        this.restTemplate.setErrorHandler(new DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(ClientHttpResponse response) {
                return false;
            }
        });
    }

    /**
     * Gửi OTP qua SMS.
     *
     * @param phone   Số điện thoại (0xxxxxxxxx, 84…, +84…)
     * @param otpCode Mã OTP (chữ số, độ dài 4–10)
     * @return {@code true} khi HTTP 2xx và JSON có {@code status: success} (chấp nhận yêu cầu gửi, không đảm bảo đã nhận trên máy)
     */
    public boolean sendOtp(String phone, String otpCode) {
        String normalizedPhone = null;
        try {
            normalizedPhone = normalizePhoneForSms(phone);
            if (normalizedPhone == null) {
                log.error("SMS OTP rejected: invalid Vietnam phone (input blank or format not supported)");
                return false;
            }

            if (!isValidOtpCode(otpCode)) {
                log.error("SMS OTP rejected: otpCode must be 4-10 digits (masked phone {})", maskPhoneForLog(normalizedPhone));
                return false;
            }

            String configError = validateTingtingConfig();
            if (configError != null) {
                log.error("SMS OTP not sent: {}", configError);
                return false;
            }

            String content = String.format(OTP_MESSAGE_TEMPLATE, otpCode.trim());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(HEADER_API_KEY, apiKey.trim());

            Map<String, String> body = new HashMap<>();
            body.put("to", normalizedPhone);
            body.put("content", content);
            body.put("sender", sender.trim());

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response;
            try {
                response = restTemplate.postForEntity(apiUrl.trim(), entity, String.class);
            } catch (RestClientException e) {
                log.error("SMS HTTP request failed for {}: {} — {}",
                        maskPhoneForLog(normalizedPhone), e.getClass().getSimpleName(), e.getMessage(), e);
                return false;
            }

            HttpStatus status = response.getStatusCode();
            String responseBody = response.getBody();

            if (!status.is2xxSuccessful()) {
                log.error("SMS Tingting returned non-2xx for {}: httpStatus={}, body={}",
                        maskPhoneForLog(normalizedPhone), status.value(), truncateForLog(responseBody));
                return false;
            }

            if (responseBody == null || responseBody.isBlank()) {
                log.error("SMS Tingting returned empty body for {} (http {})",
                        maskPhoneForLog(normalizedPhone), status.value());
                return false;
            }

            TingtingSendOutcome outcome = parseTingtingSendResponse(responseBody);
            if (outcome.success) {
                log.info(
                        "Tingting accepted SMS request (API success ≠ delivered to phone): tranId={}, smsCount={}, cost={}, to={}",
                        outcome.tranId,
                        outcome.smsCount.isEmpty() ? "?" : outcome.smsCount,
                        outcome.cost.isEmpty() ? "?" : outcome.cost,
                        maskPhoneForLog(normalizedPhone));
                return true;
            }

            log.error("SMS Tingting business error for {}: {}", maskPhoneForLog(normalizedPhone), outcome.detailForLog());
            return false;

        } catch (JsonProcessingException e) {
            log.error("SMS Tingting returned invalid JSON for {}: {}",
                    normalizedPhone != null ? maskPhoneForLog(normalizedPhone) : "(no phone)",
                    e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("SMS unexpected error for {}: {} — {}",
                    normalizedPhone != null ? maskPhoneForLog(normalizedPhone) : "(no phone)",
                    e.getClass().getSimpleName(), e.getMessage(), e);
            return false;
        }
    }

    private String validateTingtingConfig() {
        if (apiKey == null || apiKey.isBlank()) {
            return "tingting.api.key is empty";
        }
        if (apiUrl == null || apiUrl.isBlank()) {
            return "tingting.api.url is empty";
        }
        if (sender == null || sender.isBlank()) {
            return "tingting.api.sender (brandname) is empty";
        }
        String s = sender.trim();
        if (s.length() > SENDER_MAX_LENGTH) {
            return "tingting.api.sender exceeds " + SENDER_MAX_LENGTH + " characters (Tingting limit)";
        }
        return null;
    }

    private static boolean isValidOtpCode(String otpCode) {
        if (otpCode == null) {
            return false;
        }
        String t = otpCode.trim();
        return !t.isEmpty() && t.matches(OTP_PATTERN);
    }

    /**
     * Phân tích body JSON của POST /api/sms. Tingting: success có {@code status}, lỗi có {@code status: error}.
     */
    private TingtingSendOutcome parseTingtingSendResponse(String responseBody) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(responseBody);

        if (!root.has(JSON_STATUS) || root.get(JSON_STATUS).isNull()) {
            return TingtingSendOutcome.unexpected("missing '" + JSON_STATUS + "' field", responseBody);
        }

        String status = root.get(JSON_STATUS).asText().trim();
        if (STATUS_SUCCESS.equalsIgnoreCase(status)) {
            String tranId = jsonText(root, JSON_TRAN_ID);
            String smsCount = jsonText(root, JSON_SMS);
            String cost = jsonText(root, JSON_COST);
            return TingtingSendOutcome.ok(tranId, smsCount, cost);
        }

        if (STATUS_ERROR.equalsIgnoreCase(status)) {
            String code = root.has(JSON_CODE) ? root.get(JSON_CODE).asText() : "";
            String message = root.has(JSON_MESSAGE) ? root.get(JSON_MESSAGE).asText() : "";
            return TingtingSendOutcome.apiError(code, message);
        }

        return TingtingSendOutcome.unexpected("status='" + status + "'", responseBody);
    }

    private static String jsonText(JsonNode root, String field) {
        if (!root.has(field) || root.get(field).isNull()) {
            return "";
        }
        return root.get(field).asText();
    }

    private static String truncateForLog(String s) {
        if (s == null) {
            return "null";
        }
        String t = s.strip();
        if (t.length() <= LOG_BODY_MAX) {
            return t;
        }
        return t.substring(0, LOG_BODY_MAX) + "…(truncated," + t.length() + " chars)";
    }

    /**
     * Ẩn phần giữa số để log (không log full MSISDN).
     */
    private static String maskPhoneForLog(String normalized84) {
        if (normalized84 == null || normalized84.length() < 8) {
            return "(invalid)";
        }
        return normalized84.substring(0, 3) + "****" + normalized84.substring(normalized84.length() - 3);
    }

    /**
     * Chuẩn hóa về dạng 84xxxxxxxxx theo Tingting.
     */
    private String normalizePhoneForSms(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        String cleanedPhone = phone.trim().replaceAll("[\\s\\-\\(\\)]", "");

        if (cleanedPhone.startsWith("+84")) {
            cleanedPhone = "84" + cleanedPhone.substring(3);
        } else if (cleanedPhone.startsWith("0")) {
            cleanedPhone = "84" + cleanedPhone.substring(1);
        } else if (!cleanedPhone.startsWith("84")) {
            return null;
        }

        if (!PhoneValidator.validateVietnamPhoneNumber(phone)) {
            return null;
        }

        return cleanedPhone;
    }

    private static final class TingtingSendOutcome {
        final boolean success;
        final String tranId;
        /** Số SMS (theo response Tingting), rỗng nếu API không trả. */
        final String smsCount;
        final String cost;
        final String errorCode;
        final String errorMessage;
        final String unexpectedDetail;
        final String rawSnippet;

        private TingtingSendOutcome(boolean success, String tranId, String smsCount, String cost,
                String errorCode, String errorMessage, String unexpectedDetail, String rawSnippet) {
            this.success = success;
            this.tranId = tranId;
            this.smsCount = smsCount != null ? smsCount : "";
            this.cost = cost != null ? cost : "";
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
            this.unexpectedDetail = unexpectedDetail;
            this.rawSnippet = rawSnippet;
        }

        static TingtingSendOutcome ok(String tranId, String smsCount, String cost) {
            return new TingtingSendOutcome(true, Objects.toString(tranId, ""),
                    smsCount, cost, null, null, null, null);
        }

        static TingtingSendOutcome apiError(String code, String message) {
            return new TingtingSendOutcome(false, null, null, null, code, message, null, null);
        }

        static TingtingSendOutcome unexpected(String detail, String rawBody) {
            return new TingtingSendOutcome(false, null, null, null, null, null, detail, truncateForLog(rawBody));
        }

        String detailForLog() {
            if (errorCode != null || errorMessage != null) {
                return "code=" + errorCode + ", message=" + errorMessage;
            }
            return "unexpected=" + unexpectedDetail + ", body=" + rawSnippet;
        }
    }
}
