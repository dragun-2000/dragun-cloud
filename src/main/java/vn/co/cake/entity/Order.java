package vn.co.cake.entity;

import javax.persistence.*;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String code;

    // Quan hệ N-1: Nhiều đơn hàng thuộc về một người dùng
    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal prepaid;
    private String status;
    private String shippingAddress;
    private String paymentMethod;
    
    private String fullName;
    private String email;
    private String phone;
    private String note;
    private String voucher;
    
    private Integer countError = 0;
    private String messageError;

    @Column(columnDefinition = "boolean default false")
    private boolean deleted;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
}
