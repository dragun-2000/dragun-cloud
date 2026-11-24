package vn.co.cake.payment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class VietQrWebhookServiceAmountCharacterizationTest {

    @Test
    void normalizeVndAmountRoundsHalfUpToZeroDecimals() {
        assertEquals(new BigDecimal("150001"), VietQrWebhookService.normalizeVndAmount(new BigDecimal("150000.5")));
        assertEquals(new BigDecimal("150000"), VietQrWebhookService.normalizeVndAmount(new BigDecimal("150000.49")));
    }

    @Test
    void amountsMatchIgnoresScaleAndRoundsBothSides() {
        assertTrue(VietQrWebhookService.amountsMatch(new BigDecimal("150000.00"), new BigDecimal("150000")));
        assertTrue(VietQrWebhookService.amountsMatch(new BigDecimal("150000.4"), new BigDecimal("149999.5")));
        assertFalse(VietQrWebhookService.amountsMatch(new BigDecimal("150000.5"), new BigDecimal("150000.4")));
    }

    @Test
    void amountsMatchReturnsFalseWhenEitherSideIsNull() {
        assertFalse(VietQrWebhookService.amountsMatch(null, BigDecimal.ONE));
        assertFalse(VietQrWebhookService.amountsMatch(BigDecimal.ONE, null));
    }
}
