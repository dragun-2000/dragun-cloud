package vn.co.cake.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.Locale;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Characterization tests for {@link BigDecimalUtil}.
 * formatMoney uses DecimalFormat with the default FORMAT locale, so the default locale is pinned to
 * Locale.US in {@link #setUp()} and restored in {@link #tearDown()}.
 */
class BigDecimalUtilTest {

    private Locale originalLocale;

    @BeforeEach
    void setUp() {
        originalLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    void tearDown() {
        Locale.setDefault(originalLocale);
    }

    @Test
    void formatMoneyOverloadsUseGroupingAndHalfEvenRounding() {
        assertEquals("1,234,567", BigDecimalUtil.formatMoney(1234567));
        assertEquals("0", BigDecimalUtil.formatMoney(0));
        assertEquals("-1,000", BigDecimalUtil.formatMoney(-1000));
        assertEquals("999", BigDecimalUtil.formatMoney("999"));
        assertEquals("150,000", BigDecimalUtil.formatMoney("150000"));
        assertEquals("1,234,567", BigDecimalUtil.formatMoney(new BigDecimal("1234567.00")));
        // current behavior: fractions are dropped using HALF_EVEN rounding
        assertEquals("1,234", BigDecimalUtil.formatMoney(new BigDecimal("1234.5")));
        assertEquals("1,236", BigDecimalUtil.formatMoney(new BigDecimal("1235.5")));
    }

    @Test
    void formatMoneyDependsOnDefaultLocale() {
        Locale.setDefault(Locale.GERMANY);
        // current behavior: grouping separator follows the JVM default locale
        assertEquals("1.234.567", BigDecimalUtil.formatMoney(1234567));
    }

    @Test
    void formatMoneyInvalidInputsThrow() {
        assertThrows(NumberFormatException.class, () -> BigDecimalUtil.formatMoney("1000.5"));
        assertThrows(NumberFormatException.class, () -> BigDecimalUtil.formatMoney("abc"));
        assertThrows(NumberFormatException.class, () -> BigDecimalUtil.formatMoney((String) null));
        assertThrows(IllegalArgumentException.class, () -> BigDecimalUtil.formatMoney((BigDecimal) null));
    }

    @Test
    void formatDoubleDropsZeroFraction() {
        assertEquals("1", BigDecimalUtil.formatDouble("1.0"));
        assertEquals("1.5", BigDecimalUtil.formatDouble("1.50"));
        assertEquals("1000", BigDecimalUtil.formatDouble("1e3"));
        assertEquals("-2", BigDecimalUtil.formatDouble("-2.000"));
        assertThrows(NumberFormatException.class, () -> BigDecimalUtil.formatDouble("abc"));
        assertThrows(NullPointerException.class, () -> BigDecimalUtil.formatDouble(null));
    }

    @Test
    void priceValidAcceptsZeroToNineHundredNinetyNineMillion() {
        assertTrue(BigDecimalUtil.priceValid("0"));
        assertTrue(BigDecimalUtil.priceValid("999999999"));
        assertFalse(BigDecimalUtil.priceValid("1000000000"));
        assertFalse(BigDecimalUtil.priceValid("-1"));
        assertFalse(BigDecimalUtil.priceValid("1.5"));
        assertFalse(BigDecimalUtil.priceValid(" 1"));
        assertFalse(BigDecimalUtil.priceValid(null));
    }

    @Test
    void discountShippingAndStockValidationBounds() {
        assertTrue(BigDecimalUtil.discountValid("0"));
        assertTrue(BigDecimalUtil.discountValid("99"));
        assertFalse(BigDecimalUtil.discountValid("100"));
        assertFalse(BigDecimalUtil.discountValid("x"));
        assertTrue(BigDecimalUtil.discountValid(99));
        assertFalse(BigDecimalUtil.discountValid(-1));
        assertFalse(BigDecimalUtil.discountValid(100));
        assertTrue(BigDecimalUtil.shippingFeeValid(0));
        assertTrue(BigDecimalUtil.shippingFeeValid(100000));
        assertFalse(BigDecimalUtil.shippingFeeValid(100001));
        assertFalse(BigDecimalUtil.shippingFeeValid(-1));
        assertTrue(BigDecimalUtil.stockQuantityValid(0L));
        assertTrue(BigDecimalUtil.stockQuantityValid(99999L));
        assertFalse(BigDecimalUtil.stockQuantityValid(100000L));
        assertFalse(BigDecimalUtil.stockQuantityValid(-1L));
        assertFalse(BigDecimalUtil.stockQuantityValid(null));
    }
}
