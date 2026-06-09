package vn.co.cake.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import vn.co.cake.request.AccountRequest;

import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service quản lý OTP verification cho đăng ký tài khoản (in-memory).
 */
@Slf4j
@Service
public class OtpVerificationService {

    private static final String OTP_PREFIX = "register:otp:";
    private static final String RATE_LIMIT_PREFIX = "register:rate:";

    private final ConcurrentMap<String, String> otpStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, RateLimitEntry> rateLimitStore = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;

    @Value("${speedsms.otp.expiration.minutes:5}")
    private int otpExpirationMinutes;

    @Value("${speedsms.otp.max.attempts:3}")
    private int maxOtpAttempts;

    @Value("${speedsms.otp.rate.limit.per.hour:3}")
    private int maxOtpRequestsPerHour;

    public OtpVerificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    public boolean storeRegistrationData(String phone, AccountRequest accountRequest, String otpCode) {
        try {
            String key = OTP_PREFIX + phone;

            OtpData otpData = new OtpData();
            otpData.setPhone(phone);
            otpData.setOtpCode(otpCode);
            otpData.setAccountRequest(accountRequest);
            otpData.setExpiresAt(System.currentTimeMillis() + (otpExpirationMinutes * 60L * 1000L));
            otpData.setAttemptCount(0);

            String jsonData = objectMapper.writeValueAsString(otpData);
            otpStore.put(key, jsonData);

            log.info("Stored OTP data for phone: {}", phone);
            return true;
        } catch (Exception e) {
            log.error("Error storing OTP data for phone {}: {}", phone, e.getMessage(), e);
            return false;
        }
    }

    public OtpData getRegistrationData(String phone) {
        try {
            String key = OTP_PREFIX + phone;
            String data = otpStore.get(key);
            if (data == null || data.isEmpty()) {
                return null;
            }
            OtpData otpData = objectMapper.readValue(data, OtpData.class);
            if (System.currentTimeMillis() > otpData.getExpiresAt()) {
                deleteRegistrationData(phone);
                return null;
            }
            return otpData;
        } catch (Exception e) {
            log.error("Error getting OTP data for phone {}: {}", phone, e.getMessage(), e);
            return null;
        }
    }

    public OtpData verifyOtp(String phone, String otpCode) {
        OtpData otpData = getRegistrationData(phone);

        if (otpData == null) {
            log.warn("OTP data not found for phone: {}", phone);
            return null;
        }

        if (otpData.getAttemptCount() >= maxOtpAttempts) {
            log.warn("OTP attempt limit exceeded for phone: {}", phone);
            deleteRegistrationData(phone);
            return null;
        }

        if (!otpCode.equals(otpData.getOtpCode())) {
            otpData.setAttemptCount(otpData.getAttemptCount() + 1);
            try {
                String key = OTP_PREFIX + phone;
                if (otpData.getAttemptCount() >= maxOtpAttempts) {
                    deleteRegistrationData(phone);
                } else {
                    otpStore.put(key, objectMapper.writeValueAsString(otpData));
                }
            } catch (Exception e) {
                log.error("Error updating OTP attempt for phone {}: {}", phone, e.getMessage());
            }
            log.warn("Invalid OTP for phone: {}, attempt: {}", phone, otpData.getAttemptCount());
            return null;
        }

        log.info("OTP verified successfully for phone: {}", phone);
        return otpData;
    }

    public void deleteRegistrationData(String phone) {
        try {
            String key = OTP_PREFIX + phone;
            otpStore.remove(key);
            log.info("Deleted OTP data for phone: {}", phone);
        } catch (Exception e) {
            log.error("Error deleting OTP data for phone {}: {}", phone, e.getMessage(), e);
        }
    }

    public boolean checkRateLimit(String phone) {
        try {
            String key = RATE_LIMIT_PREFIX + phone;
            long now = System.currentTimeMillis();
            RateLimitEntry entry = rateLimitStore.get(key);
            if (entry == null || now > entry.expiresAtMs) {
                rateLimitStore.put(key, new RateLimitEntry(1, now + 3600_000L));
                return true;
            }
            if (entry.count >= maxOtpRequestsPerHour) {
                log.warn("Rate limit exceeded for phone: {}", phone);
                return false;
            }
            entry.count++;
            return true;
        } catch (Exception e) {
            log.warn("Rate limit check failed, allowing request: {}", e.getMessage());
            return true;
        }
    }

    private static final class RateLimitEntry {
        private int count;
        private final long expiresAtMs;

        private RateLimitEntry(int count, long expiresAtMs) {
            this.count = count;
            this.expiresAtMs = expiresAtMs;
        }
    }

    public static class OtpData {
        private String phone;
        private String otpCode;
        private AccountRequest accountRequest;
        private long expiresAt;
        private int attemptCount;

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
