package vn.co.cake.entity;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;
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
    @Column(name = "pancake_status_name", length = 255)
    private String pancakeStatusName;
    @Column(name = "pancake_status_code")
    private Integer pancakeStatusCode;
    @Column(name = "shipping_partner", length = 64)
    private String shippingPartner;
    @Column(name = "pancake_order_id", length = 64)
    private String pancakeOrderId;
    @Column(name = "tracking_link", length = 512)
    private String trackingLink;
    @Column(name = "pancake_synced_at")
    private Date pancakeSyncedAt;
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
