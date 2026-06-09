package vn.co.cake.utils;

import java.math.BigDecimal;

import vn.co.cake.entity.Product;
import vn.co.cake.entity.Variation;

/**
 * Giá bán thực tế cho đơn hàng / thanh toán — luôn lấy từ DB, không tin giá client.
 */
public final class OrderPricingUtil {

    private OrderPricingUtil() {
    }

    /**
     * Đơn giá khách phải trả: {@code product.finalPrice} → {@code product.price} → {@code variation.retailPrice}.
     */
    public static BigDecimal resolveSellingUnitPrice(Product product, Variation variation) {
        if (product != null && product.getFinalPrice() != null
                && product.getFinalPrice().compareTo(BigDecimal.ZERO) > 0) {
            return product.getFinalPrice();
        }
        if (product != null && product.getPrice() != null
                && product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return product.getPrice();
        }
        if (variation != null && variation.getRetailPrice() != null) {
            return BigDecimal.valueOf(variation.getRetailPrice());
        }
        return BigDecimal.ZERO;
    }

    /** Giá gốc / niêm yết (hiển thị, không dùng để tính tổng thanh toán). */
    public static BigDecimal resolveListUnitPrice(Product product, Variation variation) {
        if (product != null && product.getPrice() != null
                && product.getPrice().compareTo(BigDecimal.ZERO) > 0) {
            return product.getPrice();
        }
        if (variation != null && variation.getRetailPrice() != null) {
            return BigDecimal.valueOf(variation.getRetailPrice());
        }
        return resolveSellingUnitPrice(product, variation);
    }
}
