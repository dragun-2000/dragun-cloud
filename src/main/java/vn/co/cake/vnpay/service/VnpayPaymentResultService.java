package vn.co.cake.vnpay.service;

import java.util.Calendar;
import java.util.Date;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.entity.Order;
import vn.co.cake.entity.Payment;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.payment.service.InventoryReservationService;
import vn.co.cake.payment.service.PaymentTransactionLogService;
import vn.co.cake.payment.support.OrderConfirmationMailScheduler;
import vn.co.cake.payment.support.PancakeSyncScheduler;
import vn.co.cake.payment.support.PaymentFulfillmentAlertScheduler;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.repository.PaymentRepository;
import vn.co.cake.service.CartService;
import vn.co.cake.vnpay.VnpayConstants;
import vn.co.cake.vnpay.entity.VnpayPayment;

/**
 * Áp kết quả cuối cùng (đã xác thực) của một giao dịch VNPAY lên đơn hàng.
 * Gọi trong transaction của IPN hoặc QueryDR, với dòng vnpay_payment đã bị khoá.
 */
@Slf4j
@Service
public class VnpayPaymentResultService {

    private static final int CONFIRMED_RESERVATION_MINUTES = 10;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final InventoryReservationService inventoryReservationService;
    private final CartService cartService;
    private final PancakeSyncScheduler pancakeSyncScheduler;
    private final OrderConfirmationMailScheduler orderConfirmationMailScheduler;
    private final PaymentFulfillmentAlertScheduler paymentFulfillmentAlertScheduler;
    private final PaymentTransactionLogService paymentTransactionLogService;

    public VnpayPaymentResultService(OrderRepository orderRepository,
                                     PaymentRepository paymentRepository,
                                     InventoryReservationService inventoryReservationService,
                                     CartService cartService,
                                     PancakeSyncScheduler pancakeSyncScheduler,
                                     OrderConfirmationMailScheduler orderConfirmationMailScheduler,
                                     PaymentFulfillmentAlertScheduler paymentFulfillmentAlertScheduler,
                                     PaymentTransactionLogService paymentTransactionLogService) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.cartService = cartService;
        this.pancakeSyncScheduler = pancakeSyncScheduler;
        this.orderConfirmationMailScheduler = orderConfirmationMailScheduler;
        this.paymentFulfillmentAlertScheduler = paymentFulfillmentAlertScheduler;
        this.paymentTransactionLogService = paymentTransactionLogService;
    }

    /**
     * Khách đã trả tiền: xác nhận giữ kho → PENDING_SYNC → xoá giỏ, sync Pancake, gửi mail sau commit.
     * Không giữ được kho (hold đã release, hết hàng) hoặc VNPAY nghi ngờ gian lận (07) → admin xử lý.
     */
    public void applyPaid(VnpayPayment vnpay, String source, String rawPayload) {
        Order order = loadOrder(vnpay);
        markConfirmed(vnpay, source);
        createPaymentIfAbsent(order, vnpay);
        order.setPrepaid(vnpay.getAmount());
        cartService.deletedCartByAccount(vnpay.getAccountId());

        String issue = null;
        try {
            Calendar deadline = Calendar.getInstance();
            deadline.add(Calendar.MINUTE, CONFIRMED_RESERVATION_MINUTES);
            inventoryReservationService.ensureConfirmedForPaidOrder(order.getId(), deadline.getTime());
        } catch (Exception ex) {
            issue = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
        }
        if (issue == null && (VnpayConstants.CODE_SUSPICIOUS.equals(vnpay.getVnpResponseCode())
                || VnpayConstants.CODE_SUSPICIOUS.equals(vnpay.getVnpTransactionStatus()))) {
            // Đã giữ kho nhưng không tự giao: admin kiểm tra trên merchant view trước.
            issue = "VNPAY báo giao dịch nghi ngờ (07) — kiểm tra trên merchant view trước khi giao hàng";
        }

        if (issue != null) {
            vnpay.setStatus(VnpayConstants.STATUS_PAID_ISSUE);
            order.setStatus(OrderStatus.PAYMENT_RECEIVED_UNFULFILLABLE.getValue());
            order.setMessageError(issue);
            orderRepository.save(order);
            paymentTransactionLogService.logFailure(VnpayConstants.METHOD_VNPAY,
                    VnpayConstants.EVENT_REQUIRES_FULFILLMENT, vnpay.getTxnRef(), order.getId(), order.getCode(),
                    vnpay.getAccountId(), vnpay.getAmount(), "PAID_ISSUE", issue, rawPayload, null, null, null);
            paymentFulfillmentAlertScheduler.scheduleAfterCommit(order, issue);
            log.warn("VNPAY paid but needs admin txnRef={} reason={}", vnpay.getTxnRef(), issue);
            return;
        }

        vnpay.setStatus(VnpayConstants.STATUS_PAID);
        order.setStatus(OrderStatus.PENDING_SYNC.getValue());
        order.setMessageError(null);
        orderRepository.save(order);
        pancakeSyncScheduler.scheduleSyncAfterCommit(order.getId(), order.getCode());
        orderConfirmationMailScheduler.scheduleAfterCommit(order.getId(), order.getCode());
        log.info("VNPAY paid txnRef={} orderId={} source={}", vnpay.getTxnRef(), order.getId(), source);
    }

    /** Giao dịch không thành công / hết hạn: huỷ đơn chờ thanh toán và hoàn kho. */
    public void applyUnpaid(VnpayPayment vnpay, String finalStatus, String source, String reason) {
        markConfirmed(vnpay, source);
        vnpay.setStatus(finalStatus);
        Order order = orderRepository.findById(vnpay.getOrderId()).orElse(null);
        if (order == null) {
            return;
        }
        if (OrderStatus.AWAITING_PAYMENT.getValue().equals(order.getStatus())) {
            inventoryReservationService.release(order.getId());
            order.setStatus(OrderStatus.CANCELLED.getValue());
            order.setMessageError(reason);
            orderRepository.save(order);
        }
        log.info("VNPAY unpaid txnRef={} status={} source={} reason={}",
                vnpay.getTxnRef(), finalStatus, source, reason);
    }

    private Order loadOrder(VnpayPayment vnpay) {
        return orderRepository.findWithItemsAndVariationsById(vnpay.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn của giao dịch VNPAY "
                        + vnpay.getTxnRef()));
    }

    private void markConfirmed(VnpayPayment vnpay, String source) {
        vnpay.setConfirmedBy(source);
        vnpay.setConfirmedAt(new Date());
    }

    private void createPaymentIfAbsent(Order order, VnpayPayment vnpay) {
        Payment payment = paymentRepository.findFirstByOrder_Id(order.getId());
        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
        }
        payment.setAmount(vnpay.getAmount().doubleValue());
        payment.setPaymentMethod(VnpayConstants.METHOD_VNPAY);
        payment.setPaymentStatus(VnpayConstants.PAYMENT_STATUS_SUCCESS);
        payment.setTransactionId(vnpay.getVnpTransactionNo());
        paymentRepository.save(payment);
    }
}
