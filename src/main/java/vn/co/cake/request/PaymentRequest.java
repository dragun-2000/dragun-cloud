package vn.co.cake.request;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentRequest {
    private Long orderId;
    private BigDecimal amount;
}
