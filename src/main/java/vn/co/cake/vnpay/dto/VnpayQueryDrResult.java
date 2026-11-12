package vn.co.cake.vnpay.dto;

import java.util.LinkedHashMap;
import java.util.Map;

import lombok.Data;
import vn.co.cake.vnpay.VnpayConstants;

@Data
public class VnpayQueryDrResult {

    private String responseCode;
    private String transactionStatus;
    private String transactionNo;
    private String amount;
    private String bankCode;
    private String payDate;
    private String message;
    private boolean signatureValid;
    /** Toàn bộ response (đã chuẩn hoá về String) — dùng verify checksum và ghi log. */
    private Map<String, String> raw = new LinkedHashMap<>();

    public static VnpayQueryDrResult from(Map<String, Object> response) {
        VnpayQueryDrResult result = new VnpayQueryDrResult();
        if (response != null) {
            response.forEach((key, value) -> result.raw.put(key, value == null ? null : String.valueOf(value)));
        }
        result.responseCode = result.raw.get("vnp_ResponseCode");
        result.transactionStatus = result.raw.get("vnp_TransactionStatus");
        result.transactionNo = result.raw.get("vnp_TransactionNo");
        result.amount = result.raw.get("vnp_Amount");
        result.bankCode = result.raw.get("vnp_BankCode");
        result.payDate = result.raw.get("vnp_PayDate");
        result.message = result.raw.get("vnp_Message");
        return result;
    }

    /** Truy vấn thành công và VNPAY xác nhận giao dịch đã trả tiền. */
    public boolean isPaid() {
        return VnpayConstants.CODE_SUCCESS.equals(responseCode)
                && VnpayConstants.CODE_SUCCESS.equals(transactionStatus);
    }

    /** VNPAY trả lời rõ ràng là giao dịch không thành công / không tồn tại → an toàn để huỷ đơn. */
    public boolean isDefinitelyUnpaid() {
        if (VnpayConstants.QUERYDR_NOT_FOUND.equals(responseCode)) {
            return true;
        }
        return VnpayConstants.CODE_SUCCESS.equals(responseCode)
                && transactionStatus != null
                && !VnpayConstants.CODE_SUCCESS.equals(transactionStatus)
                && !VnpayConstants.CODE_SUSPICIOUS.equals(transactionStatus);
    }
}
