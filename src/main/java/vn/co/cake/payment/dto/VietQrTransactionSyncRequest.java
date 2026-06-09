package vn.co.cake.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class VietQrTransactionSyncRequest {

    private String bankaccount;
    private Long amount;
    private String transType;
    private String content;

    @JsonProperty("transactionid")
    private String transactionId;

    @JsonProperty("transactiontime")
    private Long transactionTime;

    private String referencenumber;

    @JsonProperty("orderId")
    private String orderId;

    private String sign;
}
