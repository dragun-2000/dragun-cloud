package vn.co.cake.vnpay.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;

class VnpayFormatsTest {

    @Test
    void formatDate_usesGmtPlus7RegardlessOfJvmZone() {
        Date utcMidnight = Date.from(Instant.parse("2026-11-11T00:00:00Z"));
        assertEquals("20261111070000", VnpayFormats.formatDate(utcMidnight));
        assertEquals(utcMidnight, VnpayFormats.parseDate("20261111070000"));
    }

    @Test
    void toVnpAmount_multipliesBy100WithoutDecimals() {
        assertEquals("15000000", VnpayFormats.toVnpAmount(new BigDecimal("150000")));
        assertEquals("15000000", VnpayFormats.toVnpAmount(new BigDecimal("150000.00")));
    }

    @Test
    void amountMatches_comparesWholeVnd() {
        assertTrue(VnpayFormats.amountMatches(new BigDecimal("150000.00"), "15000000"));
        assertFalse(VnpayFormats.amountMatches(new BigDecimal("150000"), "15000100"));
        assertFalse(VnpayFormats.amountMatches(new BigDecimal("150000"), "abc"));
        assertFalse(VnpayFormats.amountMatches(null, "15000000"));
        assertNull(VnpayFormats.fromVnpAmount(null));
    }

    @Test
    void toOrderInfo_stripsDiacriticsAndSpecialCharacters() {
        assertEquals("Thanh toan don hang DB 001 Dam",
                VnpayFormats.toOrderInfo("Thanh toán đơn hàng #DB-001: Đầm!"));
    }
}
