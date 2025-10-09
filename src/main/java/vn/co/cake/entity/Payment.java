package vn.co.cake.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "payments")
public class Payment extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Quan hệ 1-1: Một đơn hàng có một thanh toán
    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    private double amount;
    private String paymentMethod;
    private String paymentStatus;
    private String transactionId;
}
