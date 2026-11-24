package vn.co.cake.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Characterization tests for {@link PhoneValidator}. All numbers are fake but format-valid.
 */
class PhoneValidatorTest {

    private static final String LOCAL_FORMAT_PHONE = "0912345678";

    @ParameterizedTest
    @ValueSource(strings = {
            "0912345678", "0381234567", "0321234567", "0391234567",
            "0561234567", "0581234567", "0591234567",
            "0701234567", "0761234567", "0771234567", "0791234567",
            "0811234567", "0861234567", "0881234567", "0891234567",
            "0901234567", "0941234567", "0961234567", "0991234567"
    })
    void validateVietnamPhoneNumberAcceptsAllowedLocalPrefixes(String phone) {
        assertTrue(PhoneValidator.validateVietnamPhoneNumber(phone));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0311234567", "0301234567", "0551234567", "0571234567",
            "0711234567", "0751234567", "0801234567", "0871234567",
            "0951234567", "0201234567", "0112345678", "1912345678"
    })
    void validateVietnamPhoneNumberRejectsDisallowedPrefixes(String phone) {
        // current behavior: 087x and 095x are rejected by the prefix whitelist
        assertFalse(PhoneValidator.validateVietnamPhoneNumber(phone));
    }

    @Test
    void validateVietnamPhoneNumberAcceptsCountryCodeWithAndWithoutPlus() {
        assertTrue(PhoneValidator.validateVietnamPhoneNumber("84912345678"));
        assertTrue(PhoneValidator.validateVietnamPhoneNumber("+84912345678"));
    }

    @Test
    void validateVietnamPhoneNumberRejectsCountryCodeFollowedByTrunkZero() {
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("+840912345678"));
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("840912345678"));
    }

    @Test
    void validateVietnamPhoneNumberRejectsWrongLengths() {
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("091234567"));
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("09123456789"));
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("+8491234567"));
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("+849123456789"));
    }

    @Test
    void validateVietnamPhoneNumberRejectsNullEmptyAndBlank() {
        assertFalse(PhoneValidator.validateVietnamPhoneNumber(null));
        assertFalse(PhoneValidator.validateVietnamPhoneNumber(""));
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("   "));
    }

    @Test
    void validateVietnamPhoneNumberStripsSpacesDashesParenthesesAndTabs() {
        assertTrue(PhoneValidator.validateVietnamPhoneNumber("091 234 5678"));
        assertTrue(PhoneValidator.validateVietnamPhoneNumber("091-234-5678"));
        assertTrue(PhoneValidator.validateVietnamPhoneNumber("(091) 234-5678"));
        assertTrue(PhoneValidator.validateVietnamPhoneNumber("  +84 912 345 678  "));
        assertTrue(PhoneValidator.validateVietnamPhoneNumber("091\t2345678"));
        // current behavior: parentheses are stripped anywhere, even unbalanced
        assertTrue(PhoneValidator.validateVietnamPhoneNumber("((0912)))345678"));
    }

    @Test
    void validateVietnamPhoneNumberDoesNotStripDotsOrLetters() {
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("0912.345.678"));
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("091234567a"));
        assertFalse(PhoneValidator.validateVietnamPhoneNumber("+84-91234567x"));
    }

    @Test
    void validatePhoneNumberDelegatesToVietnamValidation() {
        assertTrue(PhoneValidator.validatePhoneNumber(LOCAL_FORMAT_PHONE));
        assertTrue(PhoneValidator.validatePhoneNumber("+84912345678"));
        assertFalse(PhoneValidator.validatePhoneNumber(null));
        // current behavior: international non-VN numbers are rejected by the "backward compatible" method
        assertFalse(PhoneValidator.validatePhoneNumber("+12025550123"));
        assertFalse(PhoneValidator.validatePhoneNumber("0951234567"));
    }

    @Test
    void normalizeVietnamPhoneKeepsLocalFormat() {
        assertEquals(LOCAL_FORMAT_PHONE, PhoneValidator.normalizeVietnamPhone(LOCAL_FORMAT_PHONE));
        assertEquals("0381234567", PhoneValidator.normalizeVietnamPhone("0381234567"));
    }

    @Test
    void normalizeVietnamPhoneConvertsCountryCodeFormsToLocal() {
        assertEquals(LOCAL_FORMAT_PHONE, PhoneValidator.normalizeVietnamPhone("84912345678"));
        assertEquals(LOCAL_FORMAT_PHONE, PhoneValidator.normalizeVietnamPhone("+84912345678"));
        assertEquals("0812345678", PhoneValidator.normalizeVietnamPhone("84812345678"));
    }

    @Test
    void normalizeVietnamPhoneStripsSeparators() {
        assertEquals(LOCAL_FORMAT_PHONE, PhoneValidator.normalizeVietnamPhone(" +84 (912) 345-678 "));
        assertEquals(LOCAL_FORMAT_PHONE, PhoneValidator.normalizeVietnamPhone("091-234-5678"));
    }

    @Test
    void normalizeVietnamPhoneReturnsNullForInvalidInput() {
        assertNull(PhoneValidator.normalizeVietnamPhone(null));
        assertNull(PhoneValidator.normalizeVietnamPhone(""));
        assertNull(PhoneValidator.normalizeVietnamPhone("   "));
        assertNull(PhoneValidator.normalizeVietnamPhone("0951234567"));
        assertNull(PhoneValidator.normalizeVietnamPhone("091234567"));
        assertNull(PhoneValidator.normalizeVietnamPhone("+840912345678"));
    }
}
