package vn.co.cake.common;

/**
 * RequestPathConst
 */
public class RequestPathConst {

    // Home path
    public static final String HOME = "/";
    // Screen M path

    public static final String AM = "/AM";
    
    public static final String SA = "/SA";
    public static final String PRIVATE_POLICY = "/privacy-policy";
    // Error page
    public static final String PAGE_ERROR = "/error";

    // ------------------------------------------------------------------------
    // LIST PATH MANAGE
    // ------------------------------------------------------------------------

    // AM001
    public static final String AM001 = "/AM/AM001";
    public static final String AM001_LOGIN = "/AM/AM001/login";
    public static final String AM001_LOGOUT = "/AM/AM001/logout";
    public static final String AM001_FORGET_PASSWORD = "/AM/AM001/forget-password";
    public static final String AM001_SEND_MAIL = "/AM/AM001/send-mail";
    public static final String AM001_VERIFY_USER = "/AM/AM001/verify_user";
    public static final String AM001_RESET_PASSWORD = "/AM/AM001/reset_password";
    public static final String AM001_CHANGE_PASSWORD_VIEW_ACCOUNT = "/AM/AM001/change_password_view/{accountId}";
    public static final String AM001_CHANGE_PASSWORD = "/AM/AM001/change-password";

    // AM002
    public static final String AM002 = "/AM/AM002";
    public static final String AM002_01 = "/AM/AM002-01";
    public static final String AM003 = "/AM/AM003";
    public static final String AM003_01 = "/AM/AM003-01";
    public static final String AM003_01_REGISTER = "/AM/AM003-01/register";
    public static final String AM003_02 = "/AM/AM003-02/{id}";
    public static final String AM003_02_UPDATE = "/AM/AM003-02/update";
    public static final String AM003_DELETED = "/AM/AM003/deleted";
    public static final String AM003_RESTORE = "/AM/AM003/restore";
    public static final String AM003_SETTINGS = "/AM/AM003/settings";
    public static final String AM003_SETTING_DISCOUNT = "/AM/AM003/settings-discount";

    
    public static final String AM004 = "/AM/AM004";
    public static final String AM004_01 = "/AM/AM004-01";
    public static final String AM004_REGISTER = "/AM/AM004/register";
    public static final String AM004_UPDATE = "/AM/AM004/update";
    public static final String AM004_DELETED = "/AM/AM004/deleted";
    public static final String AM004_02_EDIT = "/AM/AM004-02/{ID}";
    public static final String AM004_02_SETTING = "/AM/AM004-02";
    public static final String AM004_EXPORT_EXCEL = "/AM/AM004/export-excel";

    // AM006
    public static final String AM006 = "/AM/AM006";
    public static final String AM006_RETRY_SYNC = "/AM/AM006/retry-sync";
    public static final String AM006_DELETE = "/AM/AM006/delete";

    // AM008
    public static final String AM008 = "/AM/AM008";
    public static final String AM008_UPDATE_DISCOUNT = "/AM/AM008/update-discount";

    // AM009
    public static final String AM009 = "/AM/AM009";
    public static final String AM009_UPDATE_STATUS = "/AM/AM009/update-status";
    public static final String AM009_UPDATE_ALL_STATUS = "/AM/AM009/update-all-status";

    // AM010
    public static final String AM010 = "/AM/AM010";
    public static final String AM010_DETAIL = "/AM/AM010/detail/{id}";
    public static final String AM010_ORDER_LOGS = "/AM/AM010/order-logs";

    // SA001
    public static final String SA001_01_REGISTER_VIEW = "/SA/SA001/register-view";
    public static final String SA001_LOGIN = "/SA/SA001/login";
    public static final String SA001_REGISTER = "/SA/SA001/register";
    public static final String SA001_REGISTER_SEND_OTP = "/SA/SA001/register/send-otp";
    public static final String SA001_REGISTER_VERIFY_OTP = "/SA/SA001/register/verify-otp";
    public static final String SA001_REGISTER_RESEND_OTP = "/SA/SA001/register/resend-otp";
    public static final String SA001_PROFILE = "/SA/SA001/profile";
    public static final String SA001_LOGOUT = "/SA/SA001/logout";
    public static final String SA001 = "/SA/SA001";
    public static final String SA002_FORGET_PASSWORD = "/SA/SA002/forget-password";
    public static final String SA002_SEND_MAIL = "/SA/SA002/send-mail";
    public static final String SA002_VERIFY_USER = "/SA/SA002/verify_user";
    public static final String SA002_RESET_PASSWORD = "/SA/SA002/reset_password";
    public static final String SA002_CHANGE_PASSWORD_VIEW_ACCOUNT = "/SA/SA002/change_password_view/{accountId}";
    public static final String SA002_CHANGE_PASSWORD = "/SA/SA002/change-password";

    // ------------------------------------------------------------------------
    // LIST ERROR
    // ------------------------------------------------------------------------
    public static final String PAGE_FORBIDDEN = "forward:/view/403.html";
    public static final String PAGE_NOT_FOUND = "forward:/view/404.html";
    public static final String ALL_ERROR = "forward:/view/500.html";
}
