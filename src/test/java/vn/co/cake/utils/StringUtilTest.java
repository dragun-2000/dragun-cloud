package vn.co.cake.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;

import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;

/**
 * Characterization tests for {@link StringUtil}.
 */
class StringUtilTest {

    private static final String REMOTE_ADDR = "192.168.0.9";

    @Test
    void splitStringByPatternUsesRegexAndDropsTrailingEmpties() {
        assertEquals(Arrays.asList("a", "b", "", "c"), StringUtil.splitStringByPattern("a,b,,c", ","));
        // current behavior: String.split drops trailing empty strings
        assertEquals(Arrays.asList("a", "b"), StringUtil.splitStringByPattern("a,b,,", ","));
        assertEquals(Arrays.asList("a", "b"), StringUtil.splitStringByPattern("a.b", "\\."));
        assertThrows(NullPointerException.class, () -> StringUtil.splitStringByPattern(null, ","));
    }

    @Test
    void checkPasswordValidRequiresLetterDigitSpecialAndLength8To20() {
        assertTrue(StringUtil.checkPasswordValid("abc12345!"));
        // current behavior: a space counts as the required special character
        assertTrue(StringUtil.checkPasswordValid("abc1234 "));
        assertFalse(StringUtil.checkPasswordValid("abcdefgh"));
        assertFalse(StringUtil.checkPasswordValid("abc123!"));
        assertFalse(StringUtil.checkPasswordValid("abcdefghij1234567890!"));
        assertTrue(StringUtil.checkPasswordValid("abcdefghij123456789!"));
        assertFalse(StringUtil.checkPasswordValid("abc12345!é"));
    }

    @Test
    void checkMailValidLimitsTopLevelDomainToTwoToFourChars() {
        assertTrue(StringUtil.checkMailValid("a.b+c@example.com"));
        assertTrue(StringUtil.checkMailValid("user@mail.co.jp"));
        assertFalse(StringUtil.checkMailValid("user@example.c"));
        // current behavior: TLDs longer than 4 characters are rejected
        assertFalse(StringUtil.checkMailValid("user@example.museum"));
        assertFalse(StringUtil.checkMailValid("userexample.com"));
        assertThrows(NullPointerException.class, () -> StringUtil.checkMailValid(null));
    }

    @Test
    void convertStringToArrayDeduplicatesAndSkipsEmptyButDoesNotTrim() {
        assertEquals(Collections.emptyList(), StringUtil.convertStringToArray(null));
        assertEquals(Collections.emptyList(), StringUtil.convertStringToArray(""));
        assertEquals(Arrays.asList("a", "b", "c"), StringUtil.convertStringToArray("a,b,a,,c"));
        // current behavior: whitespace is kept, so " a" and "a" are different entries
        assertEquals(Arrays.asList("a", " a", " "), StringUtil.convertStringToArray("a, a, "));
    }

    @Test
    void convertArrayToStringJoinsWithComma() {
        assertEquals("", StringUtil.convertArrayToString(null));
        assertEquals("", StringUtil.convertArrayToString(Collections.emptyList()));
        assertEquals("a,b", StringUtil.convertArrayToString(Arrays.asList("a", "b")));
        // current behavior: null elements are rendered as the text "null"
        assertEquals("a,null", StringUtil.convertArrayToString(Arrays.asList("a", null)));
    }

    @Test
    void getClientIpAddrReturnsFirstUsableHeaderOrRemoteAddr() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn(REMOTE_ADDR);
        assertEquals(REMOTE_ADDR, StringUtil.getClientIpAddr(request));

        when(request.getHeader("X-Forwarded-For")).thenReturn("UNKNOWN");
        when(request.getHeader("Proxy-Client-IP")).thenReturn("");
        when(request.getHeader("HTTP_CLIENT_IP")).thenReturn("10.0.0.3");
        assertEquals("10.0.0.3", StringUtil.getClientIpAddr(request));

        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 10.0.0.2");
        // current behavior: a multi-hop X-Forwarded-For value is returned verbatim
        assertEquals("10.0.0.1, 10.0.0.2", StringUtil.getClientIpAddr(request));
    }

    @Test
    void formatWithLeadingZerosPadsToFiveDigitsWithHalfEvenRounding() {
        assertEquals("00001", StringUtil.formatWithLeadingZeros(BigDecimal.ONE));
        assertEquals("123456", StringUtil.formatWithLeadingZeros(new BigDecimal("123456")));
        // current behavior: fractions are rounded HALF_EVEN
        assertEquals("00002", StringUtil.formatWithLeadingZeros(new BigDecimal("1.5")));
        assertEquals("00002", StringUtil.formatWithLeadingZeros(new BigDecimal("2.5")));
    }
}
