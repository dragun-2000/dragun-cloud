package vn.co.cake.repository.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.querydsl.core.types.OrderSpecifier;

import vn.co.cake.enums.ProductSortOption;

class CustomProductRepositoryImplOrderTest {

    @Test
    void newestOrdersByUpdatedThenId() {
        assertEquals(Arrays.asList("product.updated DESC", "product.id DESC"), describe(ProductSortOption.NEWEST));
    }

    @Test
    void priceSortsUseThePriceAfterDiscountWithNewestAsTieBreaker() {
        List<String> asc = describe(ProductSortOption.PRICE_ASC);
        assertEquals(3, asc.size());
        assertEquals("product.price * (100 - coalesce(product.discount, 0)) ASC", asc.get(0));
        assertEquals("product.updated DESC", asc.get(1));

        assertEquals("product.price * (100 - coalesce(product.discount, 0)) DESC",
                describe(ProductSortOption.PRICE_DESC).get(0));
    }

    @Test
    void discountAndNameSorts() {
        assertEquals(Arrays.asList("coalesce(product.discount, 0) DESC", "product.updated DESC", "product.id DESC"),
                describe(ProductSortOption.DISCOUNT_DESC));
        assertEquals("product.name ASC", describe(ProductSortOption.NAME_ASC).get(0));
    }

    private static List<String> describe(ProductSortOption option) {
        return Arrays.stream(CustomProductRepositoryImpl.orderSpecifiers(option))
                .map(CustomProductRepositoryImplOrderTest::describe)
                .collect(Collectors.toList());
    }

    private static String describe(OrderSpecifier<?> spec) {
        return spec.getTarget().toString() + " " + spec.getOrder();
    }
}
