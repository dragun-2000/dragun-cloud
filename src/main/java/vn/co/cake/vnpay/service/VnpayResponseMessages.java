package vn.co.cake.vnpay.service;

import java.util.HashMap;
import java.util.Map;

import vn.co.cake.vnpay.VnpayConstants;

/** Thông điệp hiển thị cho khách theo vnp_ResponseCode (bảng mã trong tài liệu VNPAY). */
public final class VnpayResponseMessages {

    private static final Map<String, String> RESPONSE_CODE_MESSAGES = new HashMap<>();

    static {
        RESPONSE_CODE_MESSAGES.put("00", "Giao dịch thành công");
        RESPONSE_CODE_MESSAGES.put("07", "Trừ tiền thành công. Giao dịch bị nghi ngờ, cửa hàng sẽ liên hệ xác minh");
        RESPONSE_CODE_MESSAGES.put("09", "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking tại ngân hàng");
        RESPONSE_CODE_MESSAGES.put("10", "Xác thực thông tin thẻ/tài khoản không đúng quá 3 lần");
        RESPONSE_CODE_MESSAGES.put("11", "Đã hết hạn chờ thanh toán");
        RESPONSE_CODE_MESSAGES.put("12", "Thẻ/Tài khoản bị khoá");
        RESPONSE_CODE_MESSAGES.put("13", "Nhập sai mật khẩu xác thực giao dịch (OTP)");
        RESPONSE_CODE_MESSAGES.put("24", "Khách hàng huỷ giao dịch");
        RESPONSE_CODE_MESSAGES.put("51", "Tài khoản không đủ số dư");
        RESPONSE_CODE_MESSAGES.put("65", "Tài khoản đã vượt quá hạn mức giao dịch trong ngày");
        RESPONSE_CODE_MESSAGES.put("75", "Ngân hàng thanh toán đang bảo trì");
        RESPONSE_CODE_MESSAGES.put("79", "Nhập sai mật khẩu thanh toán quá số lần quy định");
        RESPONSE_CODE_MESSAGES.put("99", "Giao dịch không thành công");
    }

    private VnpayResponseMessages() {
    }

    public static String forResponseCode(String responseCode) {
        return RESPONSE_CODE_MESSAGES.getOrDefault(responseCode, "Giao dịch không thành công");
    }

    public static String forStatus(String status, String responseCode) {
        if (VnpayConstants.STATUS_PAID.equals(status)) {
            return "Thanh toán thành công. Đơn hàng đang được xử lý.";
        }
        if (VnpayConstants.STATUS_PAID_ISSUE.equals(status)) {
            return "Đã nhận thanh toán. Cửa hàng sẽ liên hệ để xác nhận đơn hàng.";
        }
        if (VnpayConstants.STATUS_EXPIRED.equals(status)) {
            return "Phiên thanh toán đã hết hạn. Vui lòng đặt hàng lại.";
        }
        if (VnpayConstants.STATUS_FAILED.equals(status)) {
            return forResponseCode(responseCode);
        }
        return "Đang chờ kết quả thanh toán từ VNPAY...";
    }
}
