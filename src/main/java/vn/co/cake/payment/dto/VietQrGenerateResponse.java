package vn.co.cake.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VietQrGenerateResponse {

    private String bankCode;
    private String bankAccount;
    private String userBankName;
    private String amount;
    private String content;
    private String qrCode;
    private String qrLink;
    private String orderId;
    private String transactionRefId;
    private String status;
    private String message;
}
