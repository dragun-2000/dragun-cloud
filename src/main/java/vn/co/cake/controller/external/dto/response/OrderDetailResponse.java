package vn.co.cake.controller.external.dto.response;



import lombok.Data;

import org.apache.commons.lang3.StringUtils;

import vn.co.cake.entity.Account;

import vn.co.cake.entity.Order;

import vn.co.cake.enums.OrderStatus;

import vn.co.cake.utils.DateUtil;



import java.math.BigDecimal;

import java.util.Collections;

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

    private String statusDisplay;

    private String shippingAddress;

    private String paymentMethod;

    private String shippingPartner;



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



    public static OrderDetailResponse forList(Order order) {

        return new OrderDetailResponse(order, false);

    }



    public OrderDetailResponse(Order order) {

        this(order, true);

    }



    private OrderDetailResponse(Order order, boolean includeItems) {

        copyOrderFields(order);

        if (includeItems && order.getOrderItems() != null) {

            this.orderItems = order.getOrderItems().stream()

                    .map(OrderItemDetailResponse::new)

                    .collect(Collectors.toList());

        } else {

            this.orderItems = Collections.emptyList();

        }

    }



    private void copyOrderFields(Order order) {

        this.code = order.getCode();

        this.totalAmount = order.getTotalAmount();

        this.shippingFee = order.getShippingFee();

        this.status = order.getStatus();

        this.statusDisplay = resolveStatusDisplay(order);

        this.shippingAddress = order.getShippingAddress();

        this.paymentMethod = order.getPaymentMethod();

        this.shippingPartner = resolveShippingPartner(order);

        this.fullName = order.getFullName();

        this.email = order.getEmail();

        this.phone = order.getPhone();

        this.note = order.getNote();

        this.deleted = order.isDeleted();

        this.countError = order.getCountError();

        this.messageError = order.getMessageError();

        this.created = DateUtil.plusHours(order.getCreated(), 7);

    }



    private static String resolveStatusDisplay(Order order) {

        if (StringUtils.isNotBlank(order.getPancakeStatusName())) {

            return order.getPancakeStatusName();

        }

        return mapInternalStatus(order.getStatus());

    }



    private static String mapInternalStatus(String status) {

        if (StringUtils.isBlank(status)) {

            return "—";

        }

        if (OrderStatus.NEW.getValue().equals(status)) {

            return "Đã đồng bộ Pancake";

        }

        if (OrderStatus.SYNC_FAIL.getValue().equals(status)) {

            return "Đồng bộ Pancake thất bại";

        }

        if (OrderStatus.PENDING_SYNC.getValue().equals(status)) {

            return "Chờ đồng bộ Pancake";

        }

        return status;

    }



    private static String resolveShippingPartner(Order order) {

        if (StringUtils.isNotBlank(order.getShippingPartner())) {

            return order.getShippingPartner();

        }

        return "—";

    }

}


