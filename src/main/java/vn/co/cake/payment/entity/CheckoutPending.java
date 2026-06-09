package vn.co.cake.payment.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import vn.co.cake.entity.BaseEntity;

@Data
@Entity
@Table(name = "checkout_pending")
public class CheckoutPending extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "vietqr_order_id", nullable = false, unique = true, length = 13)
    private String vietqrOrderId;

    @Column(name = "account_id", nullable = false)
    private long accountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 23)
    private String content;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "request_json", nullable = false, columnDefinition = "text")
    private String requestJson;

    @Column(name = "qr_link", length = 512)
    private String qrLink;

    @Column(name = "qr_code", columnDefinition = "text")
    private String qrCode;

    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;
}
