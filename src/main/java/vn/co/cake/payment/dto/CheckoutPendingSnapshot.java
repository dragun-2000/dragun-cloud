package vn.co.cake.payment.dto;

import java.util.List;

import lombok.Data;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.request.OrderDetailRequest;

@Data
public class CheckoutPendingSnapshot {

    private OrderDetailRequest orderDetailRequest;
    private List<OrderItem> cartItems;
}
