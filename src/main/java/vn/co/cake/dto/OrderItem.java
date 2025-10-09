package vn.co.cake.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
public class OrderItem implements Serializable {
    private Long id;
    private String variationId;
    private String name;
    private int quantity;
    private BigDecimal price;
    private String image;
    private String option;
}
