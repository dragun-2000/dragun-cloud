package vn.co.cake.payment.service;



import java.util.ArrayList;

import java.util.List;



import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;



import vn.co.cake.dto.CartForm;

import vn.co.cake.dto.OrderItem;

import vn.co.cake.entity.Order;

import vn.co.cake.exception.CommonServletException;

import vn.co.cake.payment.PaymentConstants;

import vn.co.cake.payment.dto.PaymentCheckoutResponse;

import vn.co.cake.payment.support.OrderConfirmationMailScheduler;
import vn.co.cake.payment.support.PancakeSyncScheduler;

import vn.co.cake.payment.support.PaymentCheckoutFlowLog;

import vn.co.cake.request.OrderDetailRequest;

import vn.co.cake.service.CartService;

import vn.co.cake.service.OrderService;



@Service

public class CodCheckoutService {



    private static final String ORDER_SUCCESS_MESSAGE = "Order Sản phẩm thành công!";



    private final OrderService orderService;

    private final PancakeSyncScheduler pancakeSyncScheduler;

    private final OrderConfirmationMailScheduler orderConfirmationMailScheduler;

    private final CartService cartService;

    private final PaymentTransactionLogService paymentTransactionLogService;



    public CodCheckoutService(OrderService orderService,

                              PancakeSyncScheduler pancakeSyncScheduler,

                              OrderConfirmationMailScheduler orderConfirmationMailScheduler,

                              CartService cartService,

                              PaymentTransactionLogService paymentTransactionLogService) {

        this.orderService = orderService;

        this.pancakeSyncScheduler = pancakeSyncScheduler;

        this.orderConfirmationMailScheduler = orderConfirmationMailScheduler;

        this.cartService = cartService;

        this.paymentTransactionLogService = paymentTransactionLogService;

    }



    @Transactional

    public PaymentCheckoutResponse placeOrder(Long accountId, CartForm cartForm, OrderDetailRequest request)

            throws CommonServletException {

        List<OrderItem> cartItems = cartForm.getOrderItems();

        PaymentCheckoutFlowLog.step("-", 1, "COD: bắt đầu đặt hàng — accountId=%s, cartItems=%s",

                accountId, cartItems != null ? cartItems.size() : 0);



        Order order = orderService.create(accountId, cartItems, request);

        String traceId = order.getCode();



        PaymentCheckoutFlowLog.step(traceId, 2,

                "COD: đơn DB đã tạo — orderId=%s, total=%s", order.getId(), order.getTotalAmount());



        clearCart(accountId, cartForm);

        PaymentCheckoutFlowLog.step(traceId, 3, "COD: đã xóa giỏ hàng — accountId=%s", accountId);



        pancakeSyncScheduler.scheduleSyncAfterCommit(order.getId(), traceId);

        orderConfirmationMailScheduler.scheduleAfterCommit(order.getId(), traceId);

        PaymentCheckoutFlowLog.step(traceId, 3, "COD: đã lên lịch sync Pancake + email xác nhận sau commit");



        paymentTransactionLogService.logSuccess(PaymentConstants.METHOD_COD, PaymentConstants.EVENT_CHECKOUT_COD,

                null, order.getId(), order.getCode(), accountId, null, null, order.getTotalAmount(),

                null, ORDER_SUCCESS_MESSAGE, null, null);



        PaymentCheckoutResponse response = new PaymentCheckoutResponse();

        response.setResultType(PaymentConstants.RESULT_TYPE_ORDER_PLACED);

        response.setMessage(ORDER_SUCCESS_MESSAGE);

        return response;

    }



    private void clearCart(Long accountId, CartForm cartForm) {

        cartService.deletedCartByAccount(accountId);

        cartForm.setOrderItems(new ArrayList<>());

    }

}


