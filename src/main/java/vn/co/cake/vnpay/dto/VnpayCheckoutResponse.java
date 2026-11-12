package vn.co.cake.vnpay.dto;

import lombok.Data;

@Data
public class VnpayCheckoutResponse {

    private String txnRef;
    private String paymentUrl;
    private Long expiresAt;
}
