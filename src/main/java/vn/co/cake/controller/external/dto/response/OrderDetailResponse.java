package vn.co.cake.controller.external.dto.response;

import lombok.Data;
import vn.co.cake.entity.Account;
import vn.co.cake.entity.Order;
import vn.co.cake.utils.DateUtil;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class OrderDetailResponse {
    private String code;
    private Account account;
    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal prepaid;
    private String status;
    private String shippingAddress;
    private String paymentMethod;

    private String fullName;
    private String email;
    private String phone;
    private String note;
    private String voucher;
    private boolean deleted;
    private Integer countError;
    private String messageError;

    private List<OrderItemDetailResponse> orderItems;
    private Date created;

    public OrderDetailResponse(Order order) {
        this.code = order.getCode();
        this.totalAmount = order.getTotalAmount();
        this.shippingFee = order.getShippingFee();
        this.status = order.getStatus();
        this.shippingAddress = order.getShippingAddress();
        this.paymentMethod = order.getPaymentMethod();

        this.fullName = order.getFullName();
        this.email = order.getEmail();
        this.phone = order.getPhone();
        this.note = order.getNote();
        this.deleted = order.isDeleted();
        this.countError = order.getCountError();
        this.messageError = order.getMessageError();
        this.created = DateUtil.plusHours(order.getCreated(), 7);
        this.orderItems = order.getOrderItems().stream().map(OrderItemDetailResponse::new).collect(Collectors.toList());
    }
}
