package vn.co.cake.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class CartForm implements Serializable {
    private static final long serialVersionUID = -8047600044472543623L;
    private List<OrderItem> orderItems;
    
    public CartForm() {
        orderItems = new ArrayList<>();
    }

    public CartForm(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }

    public List<OrderItem> getOrderItems() {
        return orderItems;
    }

    public void setOrderItems(List<OrderItem> orderItems) {
        this.orderItems = orderItems;
    }
}
