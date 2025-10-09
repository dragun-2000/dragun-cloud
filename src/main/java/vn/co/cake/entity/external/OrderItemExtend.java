package vn.co.cake.entity.external;

import lombok.Data;

import javax.persistence.Embeddable;

@Embeddable
@Data
public class OrderItemExtend {

    private String productId;
    private int quantity;
    private double price;
    private double subtotal;
    private String imageUrl;

    // Getters and setters
}
