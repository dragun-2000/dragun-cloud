package vn.co.cake.payment.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PaymentOrderLogSummary {

    /** Khóa gộp: order_code hoặc vietqr_order_id */
    private String groupKey;
    private Date lastCreated;
    private String orderCode;
    private String vietqrOrderId;
    private String paymentMethod;
    private String status;
    private BigDecimal amount;
    private String errorMessage;
    private Long detailLogId;
}
