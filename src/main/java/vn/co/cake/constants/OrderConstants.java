package vn.co.cake.constants;

public class OrderConstants {

    public static final String STATUS_NEW = "NEW";
    public static final String STATUS_SYNC_FAIL = "SYNC_FAIL";
    public static final String STATUS_PENDING_SYNC = "PENDING_SYNC";

    public static final String VOUCHER_SHIPPING_FEE = "SHIPPING_FEE";
    public static final long FREE_SHIPPING_THRESHOLD = 2000000L;
    public static final int MAX_PANCAKE_SYNC_RETRIES = 25;

    private OrderConstants() {
    }
}

