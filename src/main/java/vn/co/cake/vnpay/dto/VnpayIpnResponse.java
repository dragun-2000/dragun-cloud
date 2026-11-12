package vn.co.cake.vnpay.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import vn.co.cake.vnpay.VnpayConstants;

/** Body JSON merchant trả cho IPN: {@code {"RspCode":"00","Message":"Confirm Success"}}. */
@Getter
@AllArgsConstructor
public class VnpayIpnResponse {

    @JsonProperty("RspCode")
    private final String rspCode;

    @JsonProperty("Message")
    private final String message;

    public static VnpayIpnResponse success() {
        return new VnpayIpnResponse(VnpayConstants.RSP_SUCCESS, "Confirm Success");
    }

    public static VnpayIpnResponse orderNotFound() {
        return new VnpayIpnResponse(VnpayConstants.RSP_ORDER_NOT_FOUND, "Order not found");
    }

    public static VnpayIpnResponse alreadyConfirmed() {
        return new VnpayIpnResponse(VnpayConstants.RSP_ALREADY_CONFIRMED, "Order already confirmed");
    }

    public static VnpayIpnResponse invalidAmount() {
        return new VnpayIpnResponse(VnpayConstants.RSP_INVALID_AMOUNT, "Invalid amount");
    }

    public static VnpayIpnResponse invalidChecksum() {
        return new VnpayIpnResponse(VnpayConstants.RSP_INVALID_CHECKSUM, "Invalid Checksum");
    }

    public static VnpayIpnResponse unknownError() {
        return new VnpayIpnResponse(VnpayConstants.RSP_UNKNOWN_ERROR, "Unknown error");
    }
}
