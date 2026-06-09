package vn.co.cake.utils;

import java.math.BigDecimal;
import java.util.List;

import vn.co.cake.entity.OrderItem;
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

    /** Giảm giá trên một đơn vị = giá niêm yết − giá bán. */
    public static BigDecimal resolveUnitDiscount(Product product, Variation variation) {
        BigDecimal list = resolveListUnitPrice(product, variation);
        BigDecimal sell = resolveSellingUnitPrice(product, variation);
        BigDecimal discount = list.subtract(sell);
        return discount.compareTo(BigDecimal.ZERO) > 0 ? discount : BigDecimal.ZERO;
    }

    /** Tổng giảm giá một dòng = đơn vị giảm × số lượng. */
    public static BigDecimal lineDiscountAmount(BigDecimal unitDiscount, int quantity) {
        if (unitDiscount == null || quantity <= 0) {
            return BigDecimal.ZERO;
        }
        return unitDiscount.multiply(BigDecimal.valueOf(quantity));
    }

    /** Tổng tiền hàng (sau giảm) = Σ(finalPrice × qty). */
    public static BigDecimal sumLineSellingTotals(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (OrderItem item : orderItems) {
            if (item == null || item.getQuantity() <= 0) {
                continue;
            }
            BigDecimal unit = item.getFinalPrice() != null ? item.getFinalPrice() : item.getPrice();
            if (unit == null) {
                continue;
            }
            sum = sum.add(unit.multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return sum;
    }

    /**
     * Tổng discount gửi Pancake / đối soát = Σ(unitDiscount × qty).
     * {@code OrderItem.discountPrice} luôn là giảm giá trên một đơn vị.
     */
    public static int sumOrderDiscountAmount(List<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (OrderItem item : orderItems) {
            if (item == null || item.getQuantity() <= 0) {
                continue;
            }
            BigDecimal unitDiscount = item.getDiscountPrice();
            if (unitDiscount == null || unitDiscount.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            total += unitDiscount.multiply(BigDecimal.valueOf(item.getQuantity())).intValue();
        }
        return total;
    }
}
