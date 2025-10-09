package vn.co.cake.common;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * BaseConst
 */
public class BaseConst {

    // Session attribute user login
    public static final String ADMIN_SESSION = "adminSession";
    public static final String USER_SESSION = "userSession";
    public static final String MANAGER_SESSION = "managerSession";
    // Redirect prefix
    public static final String REDIRECT = "redirect:";
    // Error
    public static final String ERROR = "error";
    // not found
    public static final String NOT_FOUND = "not_found";
    // User ID
    public static final String UID = "uid";
    public static final String BID = "uid";
    // Charset Shift-JIS
    public static final Charset CHARSET_SHIFT_JIS = Charset.forName("Shift-JIS");
    public static final Charset CHARSET_UTF_8 = StandardCharsets.UTF_8;
    // Set header
    public static final String CONTENT_DISPOSITION = "Content-disposition";
    public static final String ATTACHMENT_FILENAME = "attachment;filename=";
}
