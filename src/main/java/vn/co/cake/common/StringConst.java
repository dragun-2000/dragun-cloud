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

    // AM Send mail
    public static final String SEND_MAIL_CREATE_ACCOUNT_TEMPLATE = "create_account_template.html";
    public static final String SEND_MAIL_CREATE_ACCOUNT_SUBJECT = " ≪シートスmail≫本社スタッフ新規登録完了のお知らせ";
    public static final String SEND_MAIL_CREATE_ACCOUNT_SA_TEMPLATE = "sa-create_account_template.html";
    public static final String SEND_MAIL_CREATE_ACCOUNT_SA_SUBJECT = "≪シートスmail≫対象顧客の営業員登録完了のお知らせ";

    public static final String SEND_MAIL_CHANGE_PASSWORD_TEMPLATE = "change_password_template.html";
    public static final String SEND_MAIL_CHANGE_PASSWORD_SUBJECT = "≪シートスmail≫パスワード変更のお知らせ";

    public static final String SEND_MAIL_FORGET_PASSWORD_TEMPLATE = "forget_password_template.html";
    public static final String SEND_MAIL_FORGET_PASSWORD_SUBJECT = "≪シートスmail≫パスワード変更のお知らせ";
    
    public static final String SEND_MAIL_SA_FORGET_PASSWORD_TEMPLATE = "sa-forget_password_template.html";
    public static final String SEND_MAIL_SA_FORGET_PASSWORD_SUBJECT = "≪DEBASE.VN≫ĐỔI MẬT KHẨU";

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
