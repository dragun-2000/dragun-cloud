package vn.co.cake.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import vn.co.cake.request.AccountRequest;

import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * Service quản lý OTP verification cho đăng ký tài khoản.
 * Dùng StringRedisTemplate để lưu/đọc JSON tránh lỗi serialization.
 */
@Slf4j
@Service
public class OtpVerificationService {

    private static final String OTP_PREFIX = "register:otp:";
    private static final String RATE_LIMIT_PREFIX = "register:rate:";

    private final StringRedisTemplate stringRedis;
    private final ObjectMapper objectMapper;

    @Value("${speedsms.otp.expiration.minutes:5}")
    private int otpExpirationMinutes;

    @Value("${speedsms.otp.max.attempts:3}")
    private int maxOtpAttempts;

    @Value("${speedsms.otp.rate.limit.per.hour:3}")
    private int maxOtpRequestsPerHour;

    public OtpVerificationService(StringRedisTemplate stringRedis) {
        this.stringRedis = stringRedis;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate mã OTP 6 số
     * 
     * @return Mã OTP 6 số
     */
    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // Random từ 100000 đến 999999
        return String.valueOf(otp);
    }

    /**
     * Lưu thông tin đăng ký và OTP vào Redis
     * 
     * @param phone Số điện thoại
     * @param accountRequest Thông tin đăng ký
     * @param otpCode Mã OTP
     * @return true nếu lưu thành công
     */
    public boolean storeRegistrationData(String phone, AccountRequest accountRequest, String otpCode) {
        try {
            String key = OTP_PREFIX + phone;
            
            OtpData otpData = new OtpData();
            otpData.setPhone(phone);
            otpData.setOtpCode(otpCode);
            otpData.setAccountRequest(accountRequest);
            otpData.setExpiresAt(System.currentTimeMillis() + (otpExpirationMinutes * 60 * 1000));
            otpData.setAttemptCount(0);

            String jsonData = objectMapper.writeValueAsString(otpData);
            stringRedis.opsForValue().set(key, jsonData, otpExpirationMinutes, TimeUnit.MINUTES);

            log.info("Stored OTP data for phone: {}", phone);
            return true;
        } catch (Exception e) {
            log.error("Error storing OTP data for phone {}: {}", phone, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Lấy thông tin đăng ký từ Redis
     * 
     * @param phone Số điện thoại
     * @return OtpData hoặc null nếu không tìm thấy
     */
    public OtpData getRegistrationData(String phone) {
        try {
            String key = OTP_PREFIX + phone;
            String data = stringRedis.opsForValue().get(key);
            if (data == null || data.isEmpty()) {
                return null;
            }
            return objectMapper.readValue(data, OtpData.class);
        } catch (Exception e) {
            log.error("Error getting OTP data for phone {}: {}", phone, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Xác thực mã OTP
     * 
     * @param phone Số điện thoại
     * @param otpCode Mã OTP người dùng nhập
     * @return OtpData nếu hợp lệ, null nếu không hợp lệ
     */
    public OtpData verifyOtp(String phone, String otpCode) {
        OtpData otpData = getRegistrationData(phone);
        
        if (otpData == null) {
            log.warn("OTP data not found for phone: {}", phone);
            return null;
        }

        // Kiểm tra hết hạn
        if (System.currentTimeMillis() > otpData.getExpiresAt()) {
            log.warn("OTP expired for phone: {}", phone);
            deleteRegistrationData(phone);
            return null;
        }

        // Kiểm tra số lần thử
        if (otpData.getAttemptCount() >= maxOtpAttempts) {
            log.warn("OTP attempt limit exceeded for phone: {}", phone);
            deleteRegistrationData(phone);
            return null;
        }

        // Kiểm tra mã OTP
        if (!otpCode.equals(otpData.getOtpCode())) {
            otpData.setAttemptCount(otpData.getAttemptCount() + 1);
            storeRegistrationData(phone, otpData.getAccountRequest(), otpData.getOtpCode());
            log.warn("Invalid OTP for phone: {}, attempt: {}", phone, otpData.getAttemptCount());
            return null;
        }

        // OTP hợp lệ
        log.info("OTP verified successfully for phone: {}", phone);
        return otpData;
    }

    /**
     * Xóa thông tin đăng ký khỏi Redis
     * 
     * @param phone Số điện thoại
     */
    public void deleteRegistrationData(String phone) {
        try {
            String key = OTP_PREFIX + phone;
            stringRedis.delete(key);
            log.info("Deleted OTP data for phone: {}", phone);
        } catch (Exception e) {
            log.error("Error deleting OTP data for phone {}: {}", phone, e.getMessage(), e);
        }
    }

    /**
     * Kiểm tra rate limit gửi OTP (max 3 lần/giờ)
     * 
     * @param phone Số điện thoại
     * @return true nếu chưa vượt rate limit, false nếu đã vượt
     */
    public boolean checkRateLimit(String phone) {
        try {
            String key = RATE_LIMIT_PREFIX + phone;
            String data = stringRedis.opsForValue().get(key);
            if (data == null || data.isEmpty()) {
                stringRedis.opsForValue().set(key, "1", 1, TimeUnit.HOURS);
                return true;
            }
            int count = Integer.parseInt(data);
            if (count >= maxOtpRequestsPerHour) {
                log.warn("Rate limit exceeded for phone: {}", phone);
                return false;
            }
            stringRedis.opsForValue().set(key, String.valueOf(count + 1), 1, TimeUnit.HOURS);
            return true;
        } catch (Exception e) {
            log.warn("Rate limit check failed (Redis?), allowing request: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Inner class để lưu trữ dữ liệu OTP
     */
    public static class OtpData {
        private String phone;
        private String otpCode;
        private AccountRequest accountRequest;
        private long expiresAt;
        private int attemptCount;

        // Getters and Setters
        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getOtpCode() {
            return otpCode;
        }

        public void setOtpCode(String otpCode) {
            this.otpCode = otpCode;
        }

        public AccountRequest getAccountRequest() {
            return accountRequest;
        }

        public void setAccountRequest(AccountRequest accountRequest) {
            this.accountRequest = accountRequest;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
        }

        public int getAttemptCount() {
            return attemptCount;
        }

        public void setAttemptCount(int attemptCount) {
            this.attemptCount = attemptCount;
        }
    }
}
