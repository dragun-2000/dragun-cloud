package vn.co.cake.payment.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class PaymentCheckoutResponse {

    private String resultType;
    private String message;
    private String orderId;
    private String qrCode;
    private String qrLink;
    private BigDecimal amount;
    private String content;
    /** Epoch millis — hết hạn phiên VietQR (đếm ngược trên UI). */
    private Long expiresAt;
}
