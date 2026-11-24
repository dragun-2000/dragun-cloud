package vn.co.cake.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import vn.co.cake.constants.OrderConstants;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;
import vn.co.cake.entity.Voucher;
import vn.co.cake.payment.repository.InventoryReservationRepository;
import vn.co.cake.repository.AccountRepository;
import vn.co.cake.repository.CartRepository;
import vn.co.cake.repository.DistrictRepository;
import vn.co.cake.repository.OrderItemRepository;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.repository.OrderSequenceRepository;
import vn.co.cake.repository.ProductRepository;
import vn.co.cake.repository.ProvinceRepository;
import vn.co.cake.repository.VariationRepository;
import vn.co.cake.repository.VoucherRepository;
import vn.co.cake.repository.WardRepository;

class OrderServiceImplPricingTest {

    private static final int SHIPPING_FEE = 30000;

    private ProductRepository productRepository;
    private VoucherRepository voucherRepository;
    private VariationRepository variationRepository;
    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        voucherRepository = mock(VoucherRepository.class);
        variationRepository = mock(VariationRepository.class);
        service = new OrderServiceImpl(mock(OrderRepository.class), mock(AccountRepository.class),
                mock(CartRepository.class), mock(OrderItemRepository.class), mock(OrderSequenceRepository.class),
                productRepository, mock(ProvinceRepository.class), mock(DistrictRepository.class),
                mock(WardRepository.class), voucherRepository, variationRepository,
                mock(InventoryReservationRepository.class));
    }

    private static OrderItem item(String variationId, int quantity) {
        OrderItem item = new OrderItem();
        item.setVariationId(variationId);
        item.setQuantity(quantity);
        // client price must be ignored by the server-side calculation
        item.setPrice(new BigDecimal("1"));
        return item;
    }

    private void givenVariation(String variationId, String pancakeProductId, Long retailPrice, String finalPrice) {
        Variation variation = new Variation();
        variation.setVariationId(variationId);
        variation.setPancakeProductId(pancakeProductId);
        variation.setRetailPrice(retailPrice);
        when(variationRepository.findAllByVariationIdIn(anyList())).thenReturn(Collections.singletonList(variation));
        if (finalPrice != null) {
            Product product = new Product();
            product.setFinalPrice(new BigDecimal(finalPrice));
            when(productRepository.findFirstByProductPancakeIdAndDeletedIsFalse(pancakeProductId)).thenReturn(product);
        }
    }

    private void givenShippingVoucher(Integer shippingFee) {
        Voucher voucher = new Voucher();
        voucher.setCode(OrderConstants.VOUCHER_SHIPPING_FEE);
        voucher.setShippingFee(shippingFee);
        when(voucherRepository.findFirstByCodeAndDeletedIsFalse(OrderConstants.VOUCHER_SHIPPING_FEE))
                .thenReturn(voucher);
    }

    @Test
    void calculateItemsSubtotal_returnsZeroWithoutLookupForNullEmptyOrMissingVariationIds() {
        assertEquals(BigDecimal.ZERO, service.calculateItemsSubtotal(null));
        assertEquals(BigDecimal.ZERO, service.calculateItemsSubtotal(Collections.emptyList()));
        assertEquals(BigDecimal.ZERO, service.calculateItemsSubtotal(Collections.singletonList(item(null, 2))));
        verify(variationRepository, never()).findAllByVariationIdIn(anyList());
    }

    @Test
    void calculateItemsSubtotal_usesServerSidePriceAndTruncatesEachLine() {
        givenVariation("V1", "P1", 90000L, "10.5");

        List<OrderItem> items = Arrays.asList(item("V1", 3), item("V1", 1));

        // current behavior: per-line longValue() truncation: 31.5 -> 31, 10.5 -> 10 => 41 (not 42)
        assertEquals(BigDecimal.valueOf(41L), service.calculateItemsSubtotal(items));
    }

    @Test
    void calculateItemsSubtotal_skipsNonPositiveQuantityAndUnknownVariations() {
        givenVariation("V1", "P1", 90000L, "100000");

        List<OrderItem> items = Arrays.asList(item("V1", 2), item("V1", 0), item("V1", -3), item("UNKNOWN", 5));

        assertEquals(BigDecimal.valueOf(200000L), service.calculateItemsSubtotal(items));
    }

    @Test
    void calculateItemsSubtotal_fallsBackToVariationRetailPriceWhenProductMissing() {
        givenVariation("V1", "P1", 90000L, null);

        assertEquals(BigDecimal.valueOf(180000L),
                service.calculateItemsSubtotal(Collections.singletonList(item("V1", 2))));
        verify(productRepository).findFirstByProductPancakeIdAndDeletedIsFalse("P1");
    }

    @Test
    void calculateGrandTotal_addsShippingFeeWhenSubtotalBelowThreshold() {
        givenVariation("V1", "P1", null, "1999999");
        givenShippingVoucher(SHIPPING_FEE);

        assertEquals(BigDecimal.valueOf(1999999L + SHIPPING_FEE),
                service.calculateGrandTotal(Collections.singletonList(item("V1", 1))));
    }

    @Test
    void calculateGrandTotal_noShippingFeeAtOrAboveThreshold() {
        givenVariation("V1", "P1", null, "2000000");
        givenShippingVoucher(SHIPPING_FEE);

        assertEquals(BigDecimal.valueOf(OrderConstants.FREE_SHIPPING_THRESHOLD),
                service.calculateGrandTotal(Collections.singletonList(item("V1", 1))));
    }

    @Test
    void calculateGrandTotal_noShippingFeeWhenShippingVoucherAbsent() {
        givenVariation("V1", "P1", null, "100000");

        assertEquals(BigDecimal.valueOf(100000L),
                service.calculateGrandTotal(Collections.singletonList(item("V1", 1))));
    }

    @Test
    void calculateGrandTotal_doesNotApplyAnyPercentageVoucherAndOnlyLooksUpShippingVoucher() {
        givenVariation("V1", "P1", null, "100000");
        givenShippingVoucher(SHIPPING_FEE);

        // current behavior: grand total = subtotal + shipping fee; no voucher discount is applied here
        assertEquals(BigDecimal.valueOf(130000L),
                service.calculateGrandTotal(Collections.singletonList(item("V1", 1))));
        verify(voucherRepository).findFirstByCodeAndDeletedIsFalse(OrderConstants.VOUCHER_SHIPPING_FEE);
    }

    @Test
    void calculateGrandTotal_throwsWhenShippingVoucherHasNullFeeBelowThreshold() {
        givenVariation("V1", "P1", null, "100000");
        givenShippingVoucher(null);

        // current behavior: Integer shippingFee unboxed without null check -> NPE
        assertThrows(NullPointerException.class,
                () -> service.calculateGrandTotal(Collections.singletonList(item("V1", 1))));
    }
}
