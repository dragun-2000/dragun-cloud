package vn.co.cake.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

class ProductSortOptionTest {

    @Test
    void fromParamMatchesKnownValuesIgnoringCase() {
        assertEquals(ProductSortOption.PRICE_ASC, ProductSortOption.fromParam("PRICE_ASC"));
        assertEquals(ProductSortOption.PRICE_DESC, ProductSortOption.fromParam("price_desc"));
        assertEquals(ProductSortOption.DISCOUNT_DESC, ProductSortOption.fromParam(" discount_desc "));
        assertEquals(ProductSortOption.NAME_ASC, ProductSortOption.fromParam("NAME_ASC"));
    }

    @Test
    void fromParamFallsBackToNewestForMissingOrUnknownValues() {
        assertEquals(ProductSortOption.NEWEST, ProductSortOption.fromParam(null));
        assertEquals(ProductSortOption.NEWEST, ProductSortOption.fromParam(""));
        assertEquals(ProductSortOption.NEWEST, ProductSortOption.fromParam("price; drop table product"));
    }

    @Test
    void customerOptionsAreNamePriceAndDiscountWithDiscountAsDefault() {
        assertEquals(Arrays.asList(ProductSortOption.NAME_ASC, ProductSortOption.PRICE_DESC,
                ProductSortOption.PRICE_ASC, ProductSortOption.DISCOUNT_DESC), ProductSortOption.CUSTOMER_OPTIONS);
        assertEquals(ProductSortOption.DISCOUNT_DESC, ProductSortOption.CUSTOMER_DEFAULT);
    }

    @Test
    void fromCustomerParamDefaultsToBiggestDiscountAndRejectsInternalOrders() {
        assertEquals(ProductSortOption.DISCOUNT_DESC, ProductSortOption.fromCustomerParam(null));
        assertEquals(ProductSortOption.DISCOUNT_DESC, ProductSortOption.fromCustomerParam("bogus"));
        assertEquals(ProductSortOption.DISCOUNT_DESC, ProductSortOption.fromCustomerParam("NEWEST"));
        assertEquals(ProductSortOption.PRICE_ASC, ProductSortOption.fromCustomerParam("price_asc"));
        assertEquals(ProductSortOption.NAME_ASC, ProductSortOption.fromCustomerParam("NAME_ASC"));
    }
}
