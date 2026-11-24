package vn.co.cake.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import vn.co.cake.request.AccountRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Characterization tests for {@link OtpVerificationService}.
 *
 * <p>The @Value configuration fields (otpExpirationMinutes, maxOtpAttempts, maxOtpRequestsPerHour)
 * have no setter or constructor, so they are set via ReflectionTestUtils.setField (configuration only).
 *
 * <p>The service reads System.currentTimeMillis() internally with no injection point. Expiry is
 * tested only deterministically via a negative expiration (already-expired on store). The 1-hour
 * rate-limit window reset cannot be tested without waiting and is not covered.
 */
class OtpVerificationServiceTest {

    private static final String PHONE = "0912345678";
    private static final String OTHER_PHONE = "0381234567";
    private static final String OTP_CODE = "123456";
    private static final String WRONG_OTP_CODE = "654321";
    private static final int EXPIRATION_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int RATE_LIMIT_PER_HOUR = 3;
    private static final String OTP_FORMAT_REGEX = "[1-9][0-9]{5}";

    private OtpVerificationService service;

    @BeforeEach
    void setUp() {
        service = newService(new ObjectMapper());
    }

    private static OtpVerificationService newService(ObjectMapper objectMapper) {
        OtpVerificationService otpService = new OtpVerificationService(objectMapper);
        ReflectionTestUtils.setField(otpService, "otpExpirationMinutes", EXPIRATION_MINUTES);
        ReflectionTestUtils.setField(otpService, "maxOtpAttempts", MAX_ATTEMPTS);
        ReflectionTestUtils.setField(otpService, "maxOtpRequestsPerHour", RATE_LIMIT_PER_HOUR);
        return otpService;
    }

    private static AccountRequest newAccountRequest() {
        AccountRequest request = new AccountRequest();
        request.setFullName("Test User");
        request.setPhone(PHONE);
        request.setMailAddress("test.user@example.com");
        request.setHeight(170);
        return request;
    }

    @Test
    void generateOtpReturnsSixDigitsWithoutLeadingZero() {
        for (int i = 0; i < 200; i++) {
            String otp = service.generateOtp();
            assertTrue(otp.matches(OTP_FORMAT_REGEX), String.format("unexpected otp format: %s", otp));
        }
    }

    @Test
    void storeRegistrationDataThenGetReturnsRoundTrippedData() {
        assertTrue(service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE));

        OtpVerificationService.OtpData data = service.getRegistrationData(PHONE);

        assertNotNull(data);
        assertEquals(PHONE, data.getPhone());
        assertEquals(OTP_CODE, data.getOtpCode());
        assertEquals(0, data.getAttemptCount());
        assertEquals("Test User", data.getAccountRequest().getFullName());
        assertEquals("test.user@example.com", data.getAccountRequest().getMailAddress());
        assertEquals(Integer.valueOf(170), data.getAccountRequest().getHeight());
        assertTrue(data.getExpiresAt() > 0L);
    }

    @Test
    void storeRegistrationDataOverwritesPreviousEntryAndResetsAttempts() {
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);
        assertNull(service.verifyOtp(PHONE, WRONG_OTP_CODE));

        service.storeRegistrationData(PHONE, newAccountRequest(), WRONG_OTP_CODE);

        OtpVerificationService.OtpData data = service.getRegistrationData(PHONE);
        assertEquals(WRONG_OTP_CODE, data.getOtpCode());
        assertEquals(0, data.getAttemptCount());
    }

    @Test
    void storeRegistrationDataReturnsFalseWhenSerializationFails() throws JsonProcessingException {
        ObjectMapper failingMapper = mock(ObjectMapper.class);
        when(failingMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") { });
        OtpVerificationService failingService = newService(failingMapper);

        assertFalse(failingService.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE));
    }

    @Test
    void getRegistrationDataReturnsNullWhenNothingStored() {
        assertNull(service.getRegistrationData(PHONE));
    }

    @Test
    void getRegistrationDataReturnsNullAndDeletesWhenAlreadyExpired() {
        // negative expiration makes the entry expired at store time, so no waiting is needed
        ReflectionTestUtils.setField(service, "otpExpirationMinutes", -1);
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);

        assertNull(service.getRegistrationData(PHONE));
        assertNull(service.verifyOtp(PHONE, OTP_CODE));
    }

    @Test
    void storageIsKeyedByRawPhoneStringWithoutNormalization() {
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);

        // current behavior: "+84912345678" is a different key from "0912345678"
        assertNull(service.getRegistrationData("+84912345678"));
        assertNull(service.getRegistrationData(OTHER_PHONE));
    }

    @Test
    void verifyOtpWithCorrectCodeReturnsDataAndKeepsItStored() {
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);

        OtpVerificationService.OtpData data = service.verifyOtp(PHONE, OTP_CODE);

        assertNotNull(data);
        assertEquals(OTP_CODE, data.getOtpCode());
        // current behavior: successful verification does not remove the data; it can be verified again
        assertNotNull(service.getRegistrationData(PHONE));
        assertNotNull(service.verifyOtp(PHONE, OTP_CODE));
    }

    @Test
    void verifyOtpReturnsNullWhenNoDataStored() {
        assertNull(service.verifyOtp(PHONE, OTP_CODE));
    }

    @Test
    void verifyOtpWithWrongCodeIncrementsPersistedAttemptCount() {
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);

        assertNull(service.verifyOtp(PHONE, WRONG_OTP_CODE));
        assertEquals(1, service.getRegistrationData(PHONE).getAttemptCount());

        assertNull(service.verifyOtp(PHONE, WRONG_OTP_CODE));
        assertEquals(2, service.getRegistrationData(PHONE).getAttemptCount());
    }

    @Test
    void verifyOtpSucceedsAfterFewerWrongAttemptsThanMaxAndReportsAttemptCount() {
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);
        service.verifyOtp(PHONE, WRONG_OTP_CODE);
        service.verifyOtp(PHONE, WRONG_OTP_CODE);

        OtpVerificationService.OtpData data = service.verifyOtp(PHONE, OTP_CODE);

        assertNotNull(data);
        assertEquals(2, data.getAttemptCount());
    }

    @Test
    void verifyOtpDeletesDataOnReachingMaxWrongAttempts() {
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);

        for (int i = 0; i < MAX_ATTEMPTS; i++) {
            assertNull(service.verifyOtp(PHONE, WRONG_OTP_CODE));
        }

        assertNull(service.getRegistrationData(PHONE));
        assertNull(service.verifyOtp(PHONE, OTP_CODE));
    }

    @Test
    void verifyOtpRejectsAndDeletesWhenStoredAttemptsAlreadyAtLoweredMax() {
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);
        service.verifyOtp(PHONE, WRONG_OTP_CODE);
        ReflectionTestUtils.setField(service, "maxOtpAttempts", 1);

        assertNull(service.verifyOtp(PHONE, OTP_CODE));
        assertNull(service.getRegistrationData(PHONE));
    }

    @Test
    void verifyOtpWithNullCodeThrowsNullPointerExceptionWhenDataExists() {
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);

        // current behavior: otpCode.equals(...) is called on the argument, so null is not handled
        assertThrows(NullPointerException.class, () -> service.verifyOtp(PHONE, null));
    }

    @Test
    void deleteRegistrationDataRemovesOnlyThatPhoneAndIgnoresMissing() {
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);
        service.storeRegistrationData(OTHER_PHONE, newAccountRequest(), OTP_CODE);

        service.deleteRegistrationData(PHONE);
        service.deleteRegistrationData("0991234567");

        assertNull(service.getRegistrationData(PHONE));
        assertNotNull(service.getRegistrationData(OTHER_PHONE));
    }

    @Test
    void checkRateLimitAllowsThreeRequestsThenRejects() {
        assertTrue(service.checkRateLimit(PHONE));
        assertTrue(service.checkRateLimit(PHONE));
        assertTrue(service.checkRateLimit(PHONE));

        assertFalse(service.checkRateLimit(PHONE));
        assertFalse(service.checkRateLimit(PHONE));
    }

    @Test
    void checkRateLimitIsTrackedPerPhone() {
        for (int i = 0; i < RATE_LIMIT_PER_HOUR; i++) {
            service.checkRateLimit(PHONE);
        }

        assertFalse(service.checkRateLimit(PHONE));
        assertTrue(service.checkRateLimit(OTHER_PHONE));
    }

    @Test
    void checkRateLimitAlwaysAllowsFirstRequestEvenWhenLimitIsZero() {
        ReflectionTestUtils.setField(service, "maxOtpRequestsPerHour", 0);

        // current behavior: the first request in a window always passes regardless of the configured limit
        assertTrue(service.checkRateLimit(PHONE));
        assertFalse(service.checkRateLimit(PHONE));
    }

    @Test
    void checkRateLimitIsIndependentOfOtpStore() {
        for (int i = 0; i < RATE_LIMIT_PER_HOUR; i++) {
            service.checkRateLimit(PHONE);
        }
        service.storeRegistrationData(PHONE, newAccountRequest(), OTP_CODE);
        service.deleteRegistrationData(PHONE);

        assertFalse(service.checkRateLimit(PHONE));
    }
}
