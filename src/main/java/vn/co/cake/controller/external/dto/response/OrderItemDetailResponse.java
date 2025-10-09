package vn.co.cake.controller.external.dto.response;

import lombok.Data;
import vn.co.cake.entity.OrderItem;
import vn.co.cake.entity.Variation;
import vn.co.cake.utils.BigDecimalUtil;

@Data
public class OrderItemDetailResponse {
    private Variation variation;
    private int quantity;
    private Long price;
    private String discountPrice;
    private String finalPrice;
    private String option;
    private String productName;
    private String displayId;

    public OrderItemDetailResponse(OrderItem orderItem) {
        this.quantity = orderItem.getQuantity();
        this.price = orderItem.getVariation().getRetailPrice();
        this.discountPrice = BigDecimalUtil.formatMoney(orderItem.getDiscountPrice());
        this.finalPrice = BigDecimalUtil.formatMoney(orderItem.getFinalPrice());
        this.option = orderItem.getOption();
        this.productName = orderItem.getVariation().getName();
        this.displayId = orderItem.getVariation().getDisplayId();
    }
}
