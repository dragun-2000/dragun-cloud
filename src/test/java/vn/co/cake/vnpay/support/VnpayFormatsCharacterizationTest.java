package vn.co.cake.vnpay.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Date;

import org.junit.jupiter.api.Test;

class VnpayFormatsCharacterizationTest {

    @Test
    void parseDateInterpretsValueAsVietnamTime() {
        Date parsed = VnpayFormats.parseDate("20260101000000");
        assertEquals(Instant.parse("2025-12-31T17:00:00Z"), parsed.toInstant());
    }

    @Test
    void parseDateReturnsNullForNullEmptyAndBlank() {
        assertNull(VnpayFormats.parseDate(null));
        assertNull(VnpayFormats.parseDate(""));
        assertNull(VnpayFormats.parseDate("   "));
    }

    @Test
    void parseDateThrowsOnMalformedValue() {
        assertThrows(DateTimeParseException.class, () -> VnpayFormats.parseDate("2026-01-01"));
    }

    @Test
    void formatDateThrowsOnNull() {
        assertThrows(NullPointerException.class, () -> VnpayFormats.formatDate(null));
    }

    @Test
    void toVnpAmountRoundsHalfUpBeforeMultiplying() {
        assertEquals("10100", VnpayFormats.toVnpAmount(new BigDecimal("100.5")));
        assertEquals("10000", VnpayFormats.toVnpAmount(new BigDecimal("100.49")));
        assertEquals("-10100", VnpayFormats.toVnpAmount(new BigDecimal("-100.5")));
        assertEquals("0", VnpayFormats.toVnpAmount(BigDecimal.ZERO));
    }

    @Test
    void toVnpAmountThrowsOnNull() {
        assertThrows(NullPointerException.class, () -> VnpayFormats.toVnpAmount(null));
    }

    @Test
    void fromVnpAmountDividesBy100WithScale2() {
        assertEquals(new BigDecimal("150000.50"), VnpayFormats.fromVnpAmount("15000050"));
        assertEquals(new BigDecimal("1.50"), VnpayFormats.fromVnpAmount("150"));
        assertEquals(new BigDecimal("0.01"), VnpayFormats.fromVnpAmount("1"));
    }

    @Test
    void fromVnpAmountReturnsNullForNonDigitInput() {
        assertNull(VnpayFormats.fromVnpAmount(""));
        assertNull(VnpayFormats.fromVnpAmount("  "));
        // current behavior: signs, decimals and surrounding spaces are rejected (StringUtils.isNumeric)
        assertNull(VnpayFormats.fromVnpAmount("-100"));
        assertNull(VnpayFormats.fromVnpAmount("1.5"));
        assertNull(VnpayFormats.fromVnpAmount(" 100"));
    }

    @Test
    void amountMatchesRoundsExpectedHalfUp() {
        assertTrue(VnpayFormats.amountMatches(new BigDecimal("150000.49"), "15000000"));
        assertFalse(VnpayFormats.amountMatches(new BigDecimal("150000.5"), "15000000"));
        assertTrue(VnpayFormats.amountMatches(new BigDecimal("150000.5"), "15000100"));
    }

    @Test
    void amountMatchesDoesNotRoundReceivedAmount() {
        // current behavior: only the expected side is rounded; a fractional received amount never matches
        assertFalse(VnpayFormats.amountMatches(new BigDecimal("150000"), "15000050"));
        assertFalse(VnpayFormats.amountMatches(new BigDecimal("150001"), "15000050"));
    }

    @Test
    void amountMatchesReturnsFalseForNullReceived() {
        assertFalse(VnpayFormats.amountMatches(new BigDecimal("150000"), null));
    }

    @Test
    void toOrderInfoReturnsEmptyForNull() {
        assertEquals("", VnpayFormats.toOrderInfo(null));
    }

    @Test
    void toOrderInfoCollapsesWhitespaceAndTrims() {
        assertEquals("a b c", VnpayFormats.toOrderInfo("  a \t\n b___c  "));
    }

    @Test
    void toOrderInfoTruncatesTo255Characters() {
        String input = "x".repeat(300);
        assertEquals("x".repeat(255), VnpayFormats.toOrderInfo(input));
    }
}
