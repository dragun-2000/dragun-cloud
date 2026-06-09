package vn.co.cake.payment.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentCheckoutStatusResponse {

    private String status;
    private String orderId;
    private BigDecimal amount;
    private String content;
    private String qrLink;
    private String qrCode;
    private String message;
    /** true khi đã có bản ghi {@code orders} (sau webhook / sandbox). */
    private boolean orderCreated;
    /** true khi {@code checkout_pending} vẫn PENDING — mới tạo QR, chưa CK thành công. */
    private boolean awaitingPayment;
}
