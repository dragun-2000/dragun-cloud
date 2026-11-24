package vn.co.cake.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import vn.co.cake.entity.OrderItem;
import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;

class OrderPricingUtilTest {

    private static Product product(String price, String finalPrice) {
        Product product = new Product();
        product.setPrice(price == null ? null : new BigDecimal(price));
        product.setFinalPrice(finalPrice == null ? null : new BigDecimal(finalPrice));
        return product;
    }

    private static Variation variation(Long retailPrice) {
        Variation variation = new Variation();
        variation.setRetailPrice(retailPrice);
        return variation;
    }

    private static OrderItem orderItem(int quantity, String price, String finalPrice, String discountPrice) {
        OrderItem item = new OrderItem();
        item.setQuantity(quantity);
        item.setPrice(price == null ? null : new BigDecimal(price));
        item.setFinalPrice(finalPrice == null ? null : new BigDecimal(finalPrice));
        item.setDiscountPrice(discountPrice == null ? null : new BigDecimal(discountPrice));
        return item;
    }

    @Test
    void resolveSellingUnitPrice_prefersFinalPriceWhenPositive() {
        assertEquals(new BigDecimal("80000"),
                OrderPricingUtil.resolveSellingUnitPrice(product("100000", "80000"), variation(90000L)));
    }

    @Test
    void resolveSellingUnitPrice_fallsBackToPriceWhenFinalPriceZeroOrNegativeOrNull() {
        assertEquals(new BigDecimal("100000"),
                OrderPricingUtil.resolveSellingUnitPrice(product("100000", "0"), variation(90000L)));
        assertEquals(new BigDecimal("100000"),
                OrderPricingUtil.resolveSellingUnitPrice(product("100000", "-5"), variation(90000L)));
        assertEquals(new BigDecimal("100000"),
                OrderPricingUtil.resolveSellingUnitPrice(product("100000", null), variation(90000L)));
    }

    @Test
    void resolveSellingUnitPrice_fallsBackToVariationRetailPriceWhenProductPricesNotPositiveOrProductNull() {
        assertEquals(BigDecimal.valueOf(90000L),
                OrderPricingUtil.resolveSellingUnitPrice(product("0", "0"), variation(90000L)));
        assertEquals(BigDecimal.valueOf(90000L),
                OrderPricingUtil.resolveSellingUnitPrice(null, variation(90000L)));
    }

    @Test
    void resolveSellingUnitPrice_returnsZeroWhenNothingAvailable() {
        assertEquals(BigDecimal.ZERO, OrderPricingUtil.resolveSellingUnitPrice(null, null));
        assertEquals(BigDecimal.ZERO, OrderPricingUtil.resolveSellingUnitPrice(product(null, null), variation(null)));
    }

    @Test
    void resolveSellingUnitPrice_returnsZeroRetailPriceFromVariationAsIs() {
        // current behavior: a variation retailPrice of 0 is accepted (no > 0 check on the variation branch)
        assertEquals(BigDecimal.valueOf(0L),
                OrderPricingUtil.resolveSellingUnitPrice(product("0", "0"), variation(0L)));
    }

    @Test
    void resolveListUnitPrice_prefersProductPriceThenVariationThenSellingPrice() {
        assertEquals(new BigDecimal("100000"),
                OrderPricingUtil.resolveListUnitPrice(product("100000", "80000"), variation(90000L)));
        assertEquals(BigDecimal.valueOf(90000L),
                OrderPricingUtil.resolveListUnitPrice(product(null, "80000"), variation(90000L)));
        // current behavior: with no price and no variation, list price falls back to finalPrice
        assertEquals(new BigDecimal("80000"),
                OrderPricingUtil.resolveListUnitPrice(product(null, "80000"), null));
        assertEquals(BigDecimal.ZERO, OrderPricingUtil.resolveListUnitPrice(null, null));
    }

    @Test
    void resolveUnitDiscount_isListMinusSellingAndClampedAtZero() {
        assertEquals(new BigDecimal("20000"),
                OrderPricingUtil.resolveUnitDiscount(product("100000", "80000"), null));
        assertEquals(BigDecimal.ZERO,
                OrderPricingUtil.resolveUnitDiscount(product("100000", "120000"), null));
    }

    @Test
    void resolveUnitDiscount_mixesVariationListPriceWithProductFinalPriceWhenProductPriceMissing() {
        // current behavior: list comes from variation retailPrice, selling from product finalPrice
        assertEquals(new BigDecimal("10000"),
                OrderPricingUtil.resolveUnitDiscount(product(null, "80000"), variation(90000L)));
    }

    @Test
    void lineDiscountAmount_multipliesAndReturnsZeroForNullOrNonPositiveQuantity() {
        assertEquals(new BigDecimal("7.5"), OrderPricingUtil.lineDiscountAmount(new BigDecimal("2.5"), 3));
        assertEquals(BigDecimal.ZERO, OrderPricingUtil.lineDiscountAmount(null, 3));
        assertEquals(BigDecimal.ZERO, OrderPricingUtil.lineDiscountAmount(new BigDecimal("2.5"), 0));
        assertEquals(BigDecimal.ZERO, OrderPricingUtil.lineDiscountAmount(new BigDecimal("2.5"), -1));
    }

    @Test
    void sumLineSellingTotals_returnsZeroForNullOrEmpty() {
        assertEquals(BigDecimal.ZERO, OrderPricingUtil.sumLineSellingTotals(null));
        assertEquals(BigDecimal.ZERO, OrderPricingUtil.sumLineSellingTotals(Collections.emptyList()));
    }

    @Test
    void sumLineSellingTotals_usesFinalPriceElsePriceAndSkipsInvalidLines() {
        List<OrderItem> items = new ArrayList<>();
        items.add(orderItem(2, "100", "80.5", null));
        items.add(orderItem(3, "50", null, null));
        items.add(null);
        items.add(orderItem(0, "999", "999", null));
        items.add(orderItem(-1, "999", "999", null));
        items.add(orderItem(5, null, null, null));
        // 80.5 * 2 + 50 * 3 = 311.0 (scale of finalPrice kept, no rounding)
        assertEquals(new BigDecimal("311.0"), OrderPricingUtil.sumLineSellingTotals(items));
    }

    @Test
    void sumOrderDiscountAmount_truncatesEachLineToIntAndSkipsInvalidLines() {
        List<OrderItem> items = Arrays.asList(
                orderItem(1, null, null, "1.5"),
                orderItem(1, null, null, "1.5"),
                orderItem(3, null, null, "1000"),
                orderItem(2, null, null, "0"),
                orderItem(2, null, null, "-10"),
                orderItem(2, null, null, null),
                orderItem(0, null, null, "500"));
        // current behavior: per-line intValue() truncation, so 1.5 + 1.5 contributes 2, not 3
        assertEquals(3002, OrderPricingUtil.sumOrderDiscountAmount(items));
    }

    @Test
    void sumOrderDiscountAmount_returnsZeroForNullOrEmpty() {
        assertEquals(0, OrderPricingUtil.sumOrderDiscountAmount(null));
        assertEquals(0, OrderPricingUtil.sumOrderDiscountAmount(Collections.emptyList()));
    }
}
