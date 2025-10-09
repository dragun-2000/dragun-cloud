package vn.co.cake.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;
import vn.co.cake.dto.OrderItem;

import java.math.BigDecimal;

@Data
@Entity
@Table(name = "cart_items")
@NoArgsConstructor
public class CartItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Quan hệ N-1: Nhiều sản phẩm thuộc về một giỏ hàng
    @ManyToOne
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "variation_id", nullable = false)
    private Variation variation;

    private int quantity;
    
    private BigDecimal totalPrice;
    
    private String option;
    
    private String image;

    public CartItem(OrderItem orderItem, Cart cart, Variation variation, Product product) {
        this.variation = variation;
        this.quantity = orderItem.getQuantity();
        this.totalPrice = orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        this.cart = cart;
        this.option = orderItem.getOption();
        this.image = product.getImage();
    }
    
    public void update(OrderItem orderItem, Cart cart, Variation variation, Product product) {
        this.variation = variation;
        this.quantity = orderItem.getQuantity();
        this.totalPrice = orderItem.getPrice().multiply(BigDecimal.valueOf(orderItem.getQuantity()));
        this.cart = cart;
        this.option = orderItem.getOption();
        this.image = product.getImage();
    }
}
