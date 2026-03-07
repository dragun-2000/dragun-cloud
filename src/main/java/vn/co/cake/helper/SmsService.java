package vn.co.cake.helper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.co.cake.utils.PhoneValidator;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Service để gửi SMS qua SpeedSMS API
 */
@Slf4j
@Service
public class SmsService {

    @Value("${speedsms.api.token:}")
    private String accessToken;

    @Value("${speedsms.api.sender:}")
    private String sender;

    @Value("${speedsms.api.type:4}")
    private int smsType;

    @Value("${speedsms.api.url:https://api.speedsms.vn/index.php/sms/send}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public SmsService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Gửi OTP qua SMS
     * 
     * @param phone Số điện thoại (format: 0xxxxxxxxx)
     * @param otpCode Mã OTP 6 số
     * @return true nếu gửi thành công, false nếu thất bại
     */
    public boolean sendOtp(String phone, String otpCode) {
        try {
            // Normalize phone về format 84xxxxxxxxx cho SpeedSMS
            String normalizedPhone = normalizePhoneForSms(phone);
            if (normalizedPhone == null) {
                log.error("Invalid phone number format: {}", phone);
                return false;
            }

            // Tạo nội dung SMS
            String content = String.format("Ma xac thuc dang ky tai khoan cua ban la: %s. Ma co hieu luc trong 5 phut.", otpCode);

            // Tạo URL với query parameters
            String url = String.format("%s?to=%s&content=%s&type=%d&sender=%s",
                    apiUrl,
                    normalizedPhone,
                    java.net.URLEncoder.encode(content, StandardCharsets.UTF_8),
                    smsType,
                    sender != null ? java.net.URLEncoder.encode(sender, StandardCharsets.UTF_8) : "");

            // Tạo headers với Basic Authentication
            HttpHeaders headers = new HttpHeaders();
            String auth = accessToken + ":x";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Gọi API
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                // Parse JSON response
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String status = jsonNode.has("status") ? jsonNode.get("status").asText() : "";

                if ("success".equals(status)) {
                    log.info("SMS sent successfully to {} with OTP code", normalizedPhone);
                    return true;
                } else {
                    String errorCode = jsonNode.has("code") ? jsonNode.get("code").asText() : "";
                    String errorMessage = jsonNode.has("message") ? jsonNode.get("message").asText() : "";
                    log.error("Failed to send SMS to {} - Error code: {}, Message: {}", normalizedPhone, errorCode, errorMessage);
                    return false;
                }
            } else {
                log.error("Failed to send SMS to {} - HTTP Status: {}", normalizedPhone, response.getStatusCode());
                return false;
            }

        } catch (Exception e) {
            log.error("Exception while sending SMS to {}: {}", phone, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Normalize số điện thoại về format 84xxxxxxxxx cho SpeedSMS
     * 
     * @param phone Số điện thoại (có thể là 0xxxxxxxxx, 84xxxxxxxxx, +84xxxxxxxxx)
     * @return Số điện thoại đã normalize hoặc null nếu không hợp lệ
     */
    private String normalizePhoneForSms(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return null;
        }

        // Loại bỏ khoảng trắng và ký tự đặc biệt
        String cleanedPhone = phone.trim().replaceAll("[\\s\\-\\(\\)]", "");

        // Convert về format 84xxxxxxxxx
        if (cleanedPhone.startsWith("+84")) {
            cleanedPhone = "84" + cleanedPhone.substring(3);
        } else if (cleanedPhone.startsWith("0")) {
            cleanedPhone = "84" + cleanedPhone.substring(1);
        } else if (!cleanedPhone.startsWith("84")) {
            return null;
        }

        // Validate format
        if (!PhoneValidator.validateVietnamPhoneNumber(phone)) {
            return null;
        }

        return cleanedPhone;
    }

    /**
     * Lấy thông tin tài khoản SpeedSMS
     * 
     * @return JSON string chứa thông tin tài khoản
     */
    public String getUserInfo() {
        try {
            String url = "https://api.speedsms.vn/index.php/user/info";

            HttpHeaders headers = new HttpHeaders();
            String auth = accessToken + ":x";
            String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encodedAuth);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                log.error("Failed to get user info - HTTP Status: {}", response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            log.error("Exception while getting user info: {}", e.getMessage(), e);
            return null;
        }
    }
}
