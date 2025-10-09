package vn.co.cake.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "order_items")
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Quan hệ N-1: Nhiều sản phẩm thuộc về một đơn hàng
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne
    @JoinColumn(name = "variation_id", nullable = false)
    private Variation variation;

    private int quantity;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private BigDecimal finalPrice;
    private String option;
}
