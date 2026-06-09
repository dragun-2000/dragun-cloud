package vn.co.cake.payment.entity;

import java.math.BigDecimal;

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
@Table(name = "payment_transaction_log")
public class PaymentTransactionLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "payment_method", nullable = false, length = 32)
    private String paymentMethod;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "vietqr_order_id", length = 13)
    private String vietqrOrderId;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "order_code", length = 32)
    private String orderCode;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "external_txn_id", length = 64)
    private String externalTxnId;

    @Column(name = "reference_number", length = 64)
    private String referenceNumber;

    private BigDecimal amount;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "request_payload", columnDefinition = "text")
    private String requestPayload;

    @Column(name = "response_payload", columnDefinition = "text")
    private String responsePayload;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "duration_ms")
    private Long durationMs;
}
