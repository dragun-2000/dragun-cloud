package vn.co.cake.payment;

/**
 * Payment and VietQR integration constants.
 */
public final class PaymentConstants {

    public static final String METHOD_COD = "COD";
    public static final String METHOD_VIETQR = "VIETQR";
    public static final String METHOD_TRANSFER_LEGACY = "TRANSFER";

    public static final String RESULT_TYPE_ORDER_PLACED = "ORDER_PLACED";
    public static final String RESULT_TYPE_QR_PAYMENT = "QR_PAYMENT";

    public static final String CHECKOUT_STATUS_PENDING = "PENDING";
    public static final String CHECKOUT_STATUS_PAID = "PAID";
    public static final String CHECKOUT_STATUS_EXPIRED = "EXPIRED";

    public static final String LOG_STATUS_SUCCESS = "SUCCESS";
    public static final String LOG_STATUS_FAILED = "FAILED";
    /** Ghi nhận bước trung gian (tạo QR, bắt đầu checkout) — chưa thanh toán xong. */
    public static final String LOG_STATUS_INFO = "INFO";

    public static final String EVENT_CHECKOUT_COD = "CHECKOUT_COD";
    public static final String EVENT_CHECKOUT_VIETQR_START = "CHECKOUT_VIETQR_START";
    public static final String EVENT_VIETQR_GET_TOKEN = "VIETQR_GET_TOKEN";
    public static final String EVENT_VIETQR_GENERATE_QR = "VIETQR_GENERATE_QR";
    public static final String EVENT_VIETQR_WEBHOOK_SYNC = "VIETQR_WEBHOOK_SYNC";
    public static final String EVENT_VIETQR_PARTNER_GET_TOKEN = "VIETQR_PARTNER_GET_TOKEN";
    public static final String EVENT_ORDER_CREATED_AFTER_PAYMENT = "ORDER_CREATED_AFTER_PAYMENT";
    public static final String EVENT_PANCAKE_SYNC = "PANCAKE_SYNC";

    public static final String PAYMENT_STATUS_SUCCESS = "SUCCESS";

    public static final String PATH_PARTNER_TOKEN = "/vqr/api/token_generate";
    public static final String PATH_TRANSACTION_SYNC = "/vqr/bank/api/transaction-sync";

    public static final String TOKEN_TYPE_BEARER = "Bearer";
    public static final int VIETQR_TOKEN_EXPIRES_SECONDS = 300;
    public static final int VIETQR_ORDER_ID_MAX_LENGTH = 13;
    public static final int VIETQR_CONTENT_MAX_LENGTH = 23;
    public static final int VIETQR_QR_TYPE_DYNAMIC = 0;
    public static final String VIETQR_TRANS_TYPE_CREDIT = "C";

    public static final int LOG_PAYLOAD_MAX_LENGTH = 8192;

    public static final String DUPLICATE_IGNORED_MESSAGE = "DUPLICATE_IGNORED";

    /** Session: accountId đã unlock VietQR pilot (gắn với user đăng nhập). */
    public static final String SESSION_VIETQR_PILOT_ACCOUNT_ID = "VIETQR_PILOT_ACCOUNT_ID";

    private PaymentConstants() {
    }
}
