package vn.co.cake.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderConfirmationMailModel {

    private String customerName;
    private String orderCode;
    private String orderDate;
    private String paymentMethodLabel;
    private String shippingAddress;
    private String phone;
    private String email;
    private String note;
    private String subtotalDisplay;
    private String shippingFeeDisplay;
    private String grandTotalDisplay;
    /** Đã thanh toán / Chưa thanh toán / Đã thanh toán một phần */
    private String paymentStatusLabel;
    private String paidAmountDisplay;
    private String remainingAmountDisplay;
    /** Mô tả ngắn theo COD hoặc VietQR */
    private String paymentSummary;
    private boolean paidOnline;
    private boolean fullyPaid;
    private String storeUrl;
    private String storeDisplayUrl;
    private String orderHistoryUrl;

    @Builder.Default
    private List<LineItem> items = new ArrayList<>();

    @Data
    @Builder
    public static class LineItem {
        private String productName;
        private String option;
        private int quantity;
        private String unitPriceDisplay;
        private String lineTotalDisplay;
    }
}
