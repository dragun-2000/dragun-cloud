package vn.co.cake.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.Test;

import vn.co.cake.controller.external.dto.VariationResponse;
import vn.co.cake.request.ProductCreateRequest;

class ProductPricingTest {

    private static final String CATEGORIES = "CAKE";
    private static final Set<String> EMPTY = Collections.emptySet();

    private static ProductCreateRequest request(String price, String discount) {
        ProductCreateRequest request = new ProductCreateRequest();
        request.setName("Cake");
        request.setPrice(price == null ? null : new BigDecimal(price));
        request.setDiscount(discount == null ? null : new BigDecimal(discount));
        return request;
    }

    private static VariationResponse variationResponse(long retailPrice) {
        VariationResponse.Product variationProduct = new VariationResponse.Product();
        variationProduct.setName("Cake");
        variationProduct.setDisplay_id("P001");
        VariationResponse response = new VariationResponse();
        response.setProduct(variationProduct);
        response.setDisplay_id("V001");
        response.setProduct_id("pancake-1");
        response.setRetail_price(retailPrice);
        response.setImages(Collections.emptyList());
        return response;
    }

    @Test
    void createConstructor_computesDiscountAmountAndFinalPrice() {
        Product product = new Product(request("100000", "15"), CATEGORIES);

        assertEquals(new BigDecimal("100000"), product.getPrice());
        assertEquals(new BigDecimal("15"), product.getDiscount());
        // discountPrice holds the discount AMOUNT, not the discounted price
        assertEquals(new BigDecimal("15000"), product.getDiscountPrice());
        assertEquals(new BigDecimal("85000"), product.getFinalPrice());
    }

    @Test
    void createConstructor_roundsDiscountAmountHalfUpAtScaleOfPriceTimesDiscount() {
        Product product = new Product(request("99999", "15"), CATEGORIES);

        // 14999.85 -> HALF_UP at scale 0
        assertEquals(new BigDecimal("15000"), product.getDiscountPrice());
        assertEquals(new BigDecimal("84999"), product.getFinalPrice());
    }

    @Test
    void createConstructor_keepsFractionalScaleFromDiscountPercent() {
        Product product = new Product(request("100001", "12.5"), CATEGORIES);

        // current behavior: result scale = scale(price * discount) = 1; 12500.125 -> 12500.1
        assertEquals(new BigDecimal("12500.1"), product.getDiscountPrice());
        assertEquals(new BigDecimal("87500.9"), product.getFinalPrice());
    }

    @Test
    void createConstructor_ignoresDiscountBelowOnePercent() {
        Product product = new Product(request("100000", "0.5"), CATEGORIES);

        // current behavior: discount.longValue() > 0 check drops sub-1% discounts
        assertEquals(new BigDecimal("0.5"), product.getDiscount());
        assertEquals(BigDecimal.ZERO, product.getDiscountPrice());
        assertEquals(new BigDecimal("100000"), product.getFinalPrice());
    }

    @Test
    void createConstructor_zeroDiscountGivesFinalPriceEqualToPrice() {
        Product product = new Product(request("100000", "0"), CATEGORIES);

        assertEquals(BigDecimal.ZERO, product.getDiscountPrice());
        assertEquals(new BigDecimal("100000"), product.getFinalPrice());
    }

    @Test
    void createConstructor_throwsWhenDiscountNull() {
        // current behavior: null discount is defaulted for the field but then dereferenced -> NPE
        assertThrows(NullPointerException.class, () -> new Product(request("100000", null), CATEGORIES));
    }

    @Test
    void update_recomputesDiscountAmountAndFinalPrice() {
        Product product = new Product(request("100000", "0"), CATEGORIES);

        product.update(request("200000", "25"), CATEGORIES);

        assertEquals(new BigDecimal("200000"), product.getPrice());
        assertEquals(new BigDecimal("50000"), product.getDiscountPrice());
        assertEquals(new BigDecimal("150000"), product.getFinalPrice());
    }

    @Test
    void pancakeConstructor_setsNoDiscountAndFinalPriceEqualToRetailPrice() {
        Product product = new Product(variationResponse(250000L), EMPTY, EMPTY, 7L);

        assertEquals(BigDecimal.valueOf(250000L), product.getPrice());
        assertEquals(BigDecimal.ZERO, product.getDiscount());
        assertEquals(BigDecimal.ZERO, product.getDiscountPrice());
        assertEquals(BigDecimal.valueOf(250000L), product.getFinalPrice());
    }

    @Test
    void toUpdate_reappliesExistingDiscountPercentToNewRetailPrice() {
        Product product = new Product();
        product.setDiscount(new BigDecimal("10"));

        Product.toUpdate(product, variationResponse(199999L), EMPTY, EMPTY, EMPTY, 3L);

        assertEquals(BigDecimal.valueOf(199999L), product.getPrice());
        // 19999.9 -> HALF_UP at scale 0
        assertEquals(new BigDecimal("20000"), product.getDiscountPrice());
        assertEquals(new BigDecimal("179999"), product.getFinalPrice());
    }

    @Test
    void toUpdate_appliesDiscountBelowOnePercentUnlikeCreate() {
        Product product = new Product();
        product.setDiscount(new BigDecimal("0.5"));

        Product.toUpdate(product, variationResponse(100000L), EMPTY, EMPTY, EMPTY, 3L);

        // current behavior: toUpdate uses compareTo(ZERO) > 0, so 0.5% is applied (create path ignores it)
        assertEquals(new BigDecimal("500.0"), product.getDiscountPrice());
        assertEquals(new BigDecimal("99500.0"), product.getFinalPrice());
    }

    @Test
    void toUpdate_nullDiscountGivesNoDiscountAndLeavesDiscountNull() {
        Product product = new Product();

        Product.toUpdate(product, variationResponse(100000L), EMPTY, EMPTY, EMPTY, 3L);

        assertNull(product.getDiscount());
        assertEquals(BigDecimal.ZERO, product.getDiscountPrice());
        assertEquals(BigDecimal.valueOf(100000L), product.getFinalPrice());
    }
}
