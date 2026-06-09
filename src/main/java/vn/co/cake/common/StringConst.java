package vn.co.cake.common;

import java.nio.charset.Charset;

/**
 * StringConst
 */
public class StringConst {

    // Zero
    public static final String ZERO = "0";
    // One
    public static final String ONE = "1";
    // Two
    public static final String TWO = "2";
    // Three
    public static final String THREE = "3";

    // Store email templates (HTML — templates/template/mail/)
    public static final String SEND_MAIL_SA_FORGET_PASSWORD_TEMPLATE = "sa-forget_password_template.html";
    public static final String SEND_MAIL_SA_FORGET_PASSWORD_SUBJECT = "≪DEBASE.VN≫ ĐỔI MẬT KHẨU";

    public static final String SEND_MAIL_ORDER_CONFIRMATION_TEMPLATE = "order_confirmation_template.html";
    public static final String SEND_MAIL_ORDER_CONFIRMATION_SUBJECT_PREFIX = "≪DEBASE.VN≫ Xác nhận đơn hàng #";

    // Authorities
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_STAFF = "ROLE_STAFF";
    public static final String ROLE_MANAGE = "ROLE_MANAGE";
    public static final String ROLE_USER = "ROLE_USER";
    public static final String WEEKLY = "WEEKLY";
    public static final String ARRIVAL = "ARRIVAL";
    public static final String NEW_IN = "NEW IN";

    // AccountStatus
    public static final String PROVISIONAL = "PROVISIONAL";
    public static final String VALID = "VALID";
    public static final String LOCKING = "LOCKING";

    public static final String ROLE = "role";

    public static final String BOOKMARK_STAFF_KEY = "BOOKMARK_STAFF";
    
    public static final String BOOKMARK_USER_KEY = "BOOKMARK_USER";

    public static final String PREFIX_QUERY = "?";

    public static final String PASSWORD_REGEX = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[ `!@#$%^&*()_+\\-=\\[\\]{};\\':\"\\\\|,.<>\\/?~])[A-Za-z\\d `!@#$%^&*()_+\\-=\\[\\]{};\\':\"\\\\|,.<>\\/?~]{8,20}";
    public static final String MAIL_REGEX = "^[\\w\\-\\.\\+]+@([\\w-]+\\.)+[\\w-]{2,4}$";
    
    public static final String CONTENT_TYPE_TEXT_DAT_SHIFT_JIS = "application/x-pds; charset=Shift-JIS";
    public static final Charset CHARSET_SHIFT_JIS = Charset.forName("Shift-JIS");
}
