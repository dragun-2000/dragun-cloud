package vn.co.cake.vnpay.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class VnpayStatusResponse {

    private String txnRef;
    /** PENDING / PAID / PAID_ISSUE / FAILED / EXPIRED */
    private String status;
    private BigDecimal amount;
    private String responseCode;
    private String message;
}
