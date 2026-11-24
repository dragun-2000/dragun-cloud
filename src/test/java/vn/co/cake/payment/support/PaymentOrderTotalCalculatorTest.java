package vn.co.cake.payment.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.co.cake.constants.OrderConstants;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Voucher;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.repository.VoucherRepository;
import vn.co.cake.service.OrderService;

class PaymentOrderTotalCalculatorTest {

    private static final int SHIPPING_FEE = 30000;
    private static final String VOUCHER_CODE = "SALE10";
    private static final List<OrderItem> ITEMS = Collections.singletonList(new OrderItem());

    private OrderService orderService;
    private VoucherRepository voucherRepository;
    private PaymentOrderTotalCalculator calculator;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        voucherRepository = mock(VoucherRepository.class);
        calculator = new PaymentOrderTotalCalculator(orderService, voucherRepository);
        Voucher shipping = new Voucher();
        shipping.setCode(OrderConstants.VOUCHER_SHIPPING_FEE);
        shipping.setShippingFee(SHIPPING_FEE);
        when(voucherRepository.findFirstByCodeAndDeletedIsFalse(OrderConstants.VOUCHER_SHIPPING_FEE))
                .thenReturn(shipping);
    }

    private void givenSubtotal(String subtotal) {
        when(orderService.calculateItemsSubtotal(anyList())).thenReturn(new BigDecimal(subtotal));
    }

    private void givenVoucher(String code, Integer discountPercent) {
        Voucher voucher = new Voucher();
        voucher.setCode(code);
        voucher.setDiscountPercent(discountPercent);
        when(voucherRepository.findFirstByCodeAndDeletedIsFalse(code)).thenReturn(voucher);
    }

    @Test
    void calculateGrandTotalVnd_withoutVoucherCodeAddsShippingBelowThreshold() {
        givenSubtotal("100000");

        assertEquals(130000L, calculator.calculateGrandTotalVnd(ITEMS, null));
        assertEquals(130000L, calculator.calculateGrandTotalVnd(ITEMS, "   "));
        // only the shipping voucher is looked up when the code is blank
        verify(voucherRepository, times(2)).findFirstByCodeAndDeletedIsFalse(OrderConstants.VOUCHER_SHIPPING_FEE);
    }

    @Test
    void calculateGrandTotalVnd_appliesPercentageDiscountWithFloorRounding() {
        givenSubtotal("1000005");
        givenVoucher(VOUCHER_CODE, 10);

        // 100000.5 floored to 100000; subtotal < threshold so shipping is added
        assertEquals(1000005L - 100000L + SHIPPING_FEE, calculator.calculateGrandTotalVnd(ITEMS, VOUCHER_CODE));
    }

    @Test
    void calculateGrandTotalVnd_trimsVoucherCodeBeforeLookup() {
        givenSubtotal("100000");
        givenVoucher(VOUCHER_CODE, 10);

        assertEquals(100000L - 10000L + SHIPPING_FEE, calculator.calculateGrandTotalVnd(ITEMS, "  SALE10 "));
    }

    @Test
    void calculateGrandTotalVnd_ignoresUnknownNullOrNonPositivePercentVoucher() {
        givenSubtotal("100000");
        givenVoucher("NULLPCT", null);
        givenVoucher("ZEROPCT", 0);
        givenVoucher("NEGPCT", -5);

        assertEquals(130000L, calculator.calculateGrandTotalVnd(ITEMS, "UNKNOWN"));
        assertEquals(130000L, calculator.calculateGrandTotalVnd(ITEMS, "NULLPCT"));
        assertEquals(130000L, calculator.calculateGrandTotalVnd(ITEMS, "ZEROPCT"));
        assertEquals(130000L, calculator.calculateGrandTotalVnd(ITEMS, "NEGPCT"));
    }

    @Test
    void calculateGrandTotalVnd_shippingThresholdUsesSubtotalBeforeVoucherDiscount() {
        givenSubtotal("2000000");
        givenVoucher(VOUCHER_CODE, 50);

        // current behavior: discounted total 1,000,000 still gets free shipping (threshold checks pre-discount subtotal)
        assertEquals(1000000L, calculator.calculateGrandTotalVnd(ITEMS, VOUCHER_CODE));
    }

    @Test
    void calculateGrandTotalVnd_allowsDiscountAbove100PercentProducingNegativeTotal() {
        givenSubtotal("100000");
        givenVoucher(VOUCHER_CODE, 150);

        // current behavior: no cap on discountPercent -> negative goods total
        assertEquals(100000L - 150000L + SHIPPING_FEE, calculator.calculateGrandTotalVnd(ITEMS, VOUCHER_CODE));
    }

    @Test
    void calculateGrandTotalVnd_truncatesFractionalSubtotal() {
        givenSubtotal("1000.9");
        givenVoucher(VOUCHER_CODE, 10);

        // subtotal.longValue() = 1000; discount floor(100.09) = 100
        assertEquals(1000L - 100L + SHIPPING_FEE, calculator.calculateGrandTotalVnd(ITEMS, VOUCHER_CODE));
    }

    @Test
    void isCodAllowed_trueAtExactLimit() {
        givenSubtotal(String.valueOf(PaymentConstants.COD_MAX_ORDER_TOTAL_VND - SHIPPING_FEE));

        assertTrue(calculator.isCodAllowed(ITEMS, null));
    }

    @Test
    void isCodAllowed_falseAboveLimitAndTrueOnceVoucherBringsTotalUnder() {
        givenSubtotal("1600000");
        givenVoucher(VOUCHER_CODE, 10);

        assertFalse(calculator.isCodAllowed(ITEMS, null));
        // 1,600,000 - 160,000 + 30,000 = 1,470,000
        assertTrue(calculator.isCodAllowed(ITEMS, VOUCHER_CODE));
    }
}
