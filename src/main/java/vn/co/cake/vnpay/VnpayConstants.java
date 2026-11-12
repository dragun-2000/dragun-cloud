package vn.co.cake.vnpay;

import java.time.ZoneId;

/**
 * Hằng số tích hợp VNPAY (tách riêng khỏi VietQR — {@code vn.co.cake.payment}).
 * Tham chiếu: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
 */
public final class VnpayConstants {

    public static final String METHOD_VNPAY = "VNPAY";

    public static final String COMMAND_PAY = "pay";
    public static final String COMMAND_QUERYDR = "querydr";
    public static final String CURR_CODE_VND = "VND";

    /** Ngày giờ gửi/nhận VNPAY: yyyyMMddHHmmss theo GMT+7. */
    public static final String DATE_PATTERN = "yyyyMMddHHmmss";
    public static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");

    /** VNPAY nhận số tiền VND × 100. */
    public static final long AMOUNT_MULTIPLIER = 100L;

    public static final String PARAM_SECURE_HASH = "vnp_SecureHash";
    public static final String PARAM_SECURE_HASH_TYPE = "vnp_SecureHashType";

    /** vnp_ResponseCode / vnp_TransactionStatus. */
    public static final String CODE_SUCCESS = "00";
    /** Trừ tiền thành công nhưng giao dịch bị nghi ngờ (gian lận) — cần admin kiểm tra. */
    public static final String CODE_SUSPICIOUS = "07";
    public static final String QUERYDR_NOT_FOUND = "91";

    /** RspCode merchant trả cho IPN. */
    public static final String RSP_SUCCESS = "00";
    public static final String RSP_ORDER_NOT_FOUND = "01";
    public static final String RSP_ALREADY_CONFIRMED = "02";
    public static final String RSP_INVALID_AMOUNT = "04";
    public static final String RSP_INVALID_CHECKSUM = "97";
    public static final String RSP_UNKNOWN_ERROR = "99";

    /** Trạng thái bảng vnpay_payment. */
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_PAID_ISSUE = "PAID_ISSUE";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_EXPIRED = "EXPIRED";

    /** event_type ghi vào payment_transaction_log (màn hình log admin dùng chung). */
    public static final String EVENT_CHECKOUT_START = "CHECKOUT_VNPAY_START";
    public static final String EVENT_IPN = "VNPAY_IPN";
    public static final String EVENT_QUERYDR = "VNPAY_QUERYDR";
    public static final String EVENT_EXPIRED = "VNPAY_EXPIRED";
    public static final String EVENT_REQUIRES_FULFILLMENT = "VNPAY_PAYMENT_REQUIRES_FULFILLMENT";

    public static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";

    private VnpayConstants() {
    }
}
