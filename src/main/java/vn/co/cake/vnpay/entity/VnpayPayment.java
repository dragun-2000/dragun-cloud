package vn.co.cake.vnpay.entity;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;
import vn.co.cake.entity.BaseEntity;

/**
 * Một lần tạo URL thanh toán VNPAY. {@code txnRef} = vnp_TxnRef = orders.code.
 * Lưu {@code vnpCreateDate} vì QueryDR/Refund cần đúng giá trị này (vnp_TransactionDate).
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Entity
@Table(name = "vnpay_payment")
public class VnpayPayment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(name = "txn_ref", nullable = false, unique = true, length = 100)
    private String txnRef;

    @Column(name = "order_id", nullable = false)
    private long orderId;

    @Column(name = "account_id", nullable = false)
    private long accountId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 16)
    private String status;

    @Column(name = "order_info", nullable = false, length = 255)
    private String orderInfo;

    @Column(name = "client_ip", length = 45)
    private String clientIp;

    @Column(name = "vnp_create_date", nullable = false, length = 14)
    private String vnpCreateDate;

    @Column(name = "expires_at", nullable = false)
    private Date expiresAt;

    @Column(name = "vnp_transaction_no", length = 15)
    private String vnpTransactionNo;

    @Column(name = "vnp_bank_code", length = 20)
    private String vnpBankCode;

    @Column(name = "vnp_bank_tran_no", length = 255)
    private String vnpBankTranNo;

    @Column(name = "vnp_card_type", length = 20)
    private String vnpCardType;

    @Column(name = "vnp_pay_date", length = 14)
    private String vnpPayDate;

    @Column(name = "vnp_response_code", length = 2)
    private String vnpResponseCode;

    @Column(name = "vnp_transaction_status", length = 2)
    private String vnpTransactionStatus;

    /** IPN hoặc QUERYDR — nguồn đã chốt trạng thái cuối. */
    @Column(name = "confirmed_by", length = 16)
    private String confirmedBy;

    @Column(name = "confirmed_at")
    private Date confirmedAt;
}
