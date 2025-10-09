package vn.co.cake.exception;

import javax.servlet.ServletException;

/**
 * PanServletException
 */
public class CommonServletException extends ServletException {

    private static final String PERMISSION_DENIED = "アクセス拒否";
    private static final String NOT_ALLOWED_ROLE = "医療機関に担当されていないから、担当医療機関の情報を見えていません。";

    public CommonServletException(String message) {
        super(message);
    }

    public static CommonServletException permissionDenied() {
        return new CommonServletException(PERMISSION_DENIED);
    }

    public static CommonServletException notAllowedRole() {
        return new CommonServletException(NOT_ALLOWED_ROLE);
    }
}
