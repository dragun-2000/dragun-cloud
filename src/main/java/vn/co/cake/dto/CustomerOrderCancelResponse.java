package vn.co.cake.dto;

import lombok.Data;

@Data
public class CustomerOrderCancelResponse {

    public static final String RESULT_CANCELLED = "CANCELLED";
    public static final String RESULT_CONTACT_ADMIN = "CONTACT_ADMIN";
    public static final String RESULT_ALREADY_CANCELLED = "ALREADY_CANCELLED";

    private String result;
    private String orderCode;
    private String statusName;
    private String message;
    private String facebookUrl;
    private String instagramUrl;

    public static CustomerOrderCancelResponse cancelled(String orderCode, String statusName) {
        CustomerOrderCancelResponse response = new CustomerOrderCancelResponse();
        response.setResult(RESULT_CANCELLED);
        response.setOrderCode(orderCode);
        response.setStatusName(statusName != null ? statusName : "Đã hủy");
        response.setMessage("Đã hủy đơn hàng thành công.");
        return response;
    }

    public static CustomerOrderCancelResponse alreadyCancelled(String orderCode, String statusName) {
        CustomerOrderCancelResponse response = new CustomerOrderCancelResponse();
        response.setResult(RESULT_ALREADY_CANCELLED);
        response.setOrderCode(orderCode);
        response.setStatusName(statusName != null ? statusName : "Đã hủy");
        response.setMessage("Đơn hàng đã được hủy trước đó.");
        return response;
    }

    public static CustomerOrderCancelResponse contactAdmin(String orderCode, String statusName) {
        CustomerOrderCancelResponse response = new CustomerOrderCancelResponse();
        response.setResult(RESULT_CONTACT_ADMIN);
        response.setOrderCode(orderCode);
        response.setStatusName(statusName);
        response.setMessage("Đơn đã được xác nhận. Vui lòng liên hệ admin qua Facebook hoặc Instagram để hủy.");
        response.setFacebookUrl("https://www.facebook.com/debase.95");
        response.setInstagramUrl("https://www.instagram.com/debase.95");
        return response;
    }
}
