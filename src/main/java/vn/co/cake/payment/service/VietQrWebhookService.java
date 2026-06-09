package vn.co.cake.payment.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Order;
import vn.co.cake.entity.Payment;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.dto.CheckoutPendingSnapshot;
import vn.co.cake.payment.dto.VietQrTransactionSyncRequest;
import vn.co.cake.payment.entity.CheckoutPending;
import vn.co.cake.payment.repository.CheckoutPendingRepository;
import vn.co.cake.payment.repository.PaymentTransactionLogRepository;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.repository.PaymentRepository;
import vn.co.cake.request.OrderDetailRequest;
import vn.co.cake.payment.support.OrderConfirmationMailScheduler;
import vn.co.cake.payment.support.PancakeSyncScheduler;
import vn.co.cake.payment.support.PaymentCheckoutFlowLog;
import vn.co.cake.service.CartService;
import vn.co.cake.service.OrderService;

@Service
public class VietQrWebhookService {

    private final CheckoutPendingRepository checkoutPendingRepository;
    private final OrderService orderService;
    private final CartService cartService;
    private final PancakeSyncScheduler pancakeSyncScheduler;
    private final OrderConfirmationMailScheduler orderConfirmationMailScheduler;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentTransactionLogRepository paymentTransactionLogRepository;
    private final PaymentTransactionLogService paymentTransactionLogService;
    private final ObjectMapper objectMapper;

    public VietQrWebhookService(CheckoutPendingRepository checkoutPendingRepository,
                                OrderService orderService,
                                CartService cartService,
                                PancakeSyncScheduler pancakeSyncScheduler,
                                OrderConfirmationMailScheduler orderConfirmationMailScheduler,
                                PaymentRepository paymentRepository,
                                OrderRepository orderRepository,
                                PaymentTransactionLogRepository paymentTransactionLogRepository,
                                PaymentTransactionLogService paymentTransactionLogService,
                                ObjectMapper objectMapper) {
        this.checkoutPendingRepository = checkoutPendingRepository;
        this.orderService = orderService;
        this.cartService = cartService;
        this.pancakeSyncScheduler = pancakeSyncScheduler;
        this.orderConfirmationMailScheduler = orderConfirmationMailScheduler;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.paymentTransactionLogRepository = paymentTransactionLogRepository;
        this.paymentTransactionLogService = paymentTransactionLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void confirmPayment(VietQrTransactionSyncRequest payload, String requestJson) throws CommonServletException {
        String orderId = resolveVietqrOrderId(payload);
        payload.setOrderId(orderId);
        PaymentCheckoutFlowLog.step(orderId, 2,
                "Webhook xác nhận thanh toán — transType=%s, amount=%s, txnId=%s",
                payload.getTransType(), payload.getAmount(), payload.getTransactionId());

        CheckoutPending existing = checkoutPendingRepository.findFirstByVietqrOrderId(orderId).orElse(null);
        if (existing != null && PaymentConstants.CHECKOUT_STATUS_PAID.equals(existing.getStatus())) {
            PaymentCheckoutFlowLog.step(orderId, 3, "Đã PAID — bỏ qua webhook trùng (idempotent)");
            return;
        }

        if (payload.getTransactionId() != null
                && paymentTransactionLogRepository.existsByExternalTxnIdAndEventTypeAndStatus(
                payload.getTransactionId(), PaymentConstants.EVENT_VIETQR_WEBHOOK_SYNC,
                PaymentConstants.LOG_STATUS_SUCCESS)) {
            if (reconcileCheckoutAfterDuplicateWebhook(orderId, existing, payload, requestJson)) {
                return;
            }
            if (existing != null && PaymentConstants.CHECKOUT_STATUS_PENDING.equals(existing.getStatus())) {
                PaymentCheckoutFlowLog.step(orderId, 3,
                        "Webhook txn=%s đã log SUCCESS nhưng checkout vẫn PENDING — thử xử lý lại",
                        payload.getTransactionId());
            } else {
                PaymentCheckoutFlowLog.step(orderId, 3, "Bỏ qua: webhook trùng transactionId=%s", payload.getTransactionId());
                paymentTransactionLogService.logSuccess(PaymentConstants.METHOD_VIETQR,
                        PaymentConstants.EVENT_VIETQR_WEBHOOK_SYNC, orderId, null, orderId, null,
                        payload.getTransactionId(), payload.getReferencenumber(), toBigDecimal(payload.getAmount()),
                        requestJson, PaymentConstants.DUPLICATE_IGNORED_MESSAGE, 200, null);
                return;
            }
        }

        CheckoutPending pending = checkoutPendingRepository
                .findFirstByVietqrOrderIdAndStatus(orderId, PaymentConstants.CHECKOUT_STATUS_PENDING)
                .orElse(null);
        if (pending == null) {
            Order existingOrder = orderRepository.findFirstByCode(orderId);
            if (existingOrder != null && existing != null
                    && PaymentConstants.CHECKOUT_STATUS_PENDING.equals(existing.getStatus())) {
                markCheckoutPaid(existing, existingOrder, payload);
                PaymentCheckoutFlowLog.step(orderId, 3,
                        "Khôi phục checkout_pending=PAID từ đơn đã tồn tại — orderId=%s", existingOrder.getId());
                return;
            }
            PaymentCheckoutFlowLog.step(orderId, 3, "LỖI: không tìm thấy checkout_pending PENDING cho vietqrOrderId=%s", orderId);
            paymentTransactionLogService.logFailure(PaymentConstants.METHOD_VIETQR,
                    PaymentConstants.EVENT_VIETQR_WEBHOOK_SYNC, orderId, null, orderId, null,
                    toBigDecimal(payload.getAmount()), "PENDING_NOT_FOUND", "Checkout pending not found",
                    requestJson, null, 200, null);
            throw new CommonServletException("Checkout pending not found");
        }
        PaymentCheckoutFlowLog.step(orderId, 3,
                "Tìm thấy checkout_pending — accountId=%s, expectedAmount=%s",
                pending.getAccountId(), pending.getAmount());

        if (!PaymentConstants.VIETQR_TRANS_TYPE_CREDIT.equalsIgnoreCase(payload.getTransType())) {
            PaymentCheckoutFlowLog.step(orderId, 4, "LỖI: transType không hợp lệ — %s", payload.getTransType());
            paymentTransactionLogService.logFailure(PaymentConstants.METHOD_VIETQR,
                    PaymentConstants.EVENT_VIETQR_WEBHOOK_SYNC, orderId, null, orderId, pending.getAccountId(),
                    toBigDecimal(payload.getAmount()), "INVALID_TRANS_TYPE", payload.getTransType(),
                    requestJson, null, 200, null);
            throw new CommonServletException("Invalid transaction type");
        }

        BigDecimal receivedAmount = toBigDecimal(payload.getAmount());
        if (!amountsMatch(pending.getAmount(), receivedAmount)) {
            PaymentCheckoutFlowLog.step(orderId, 4,
                    "LỖI: số tiền không khớp — expected=%s, received=%s", pending.getAmount(), receivedAmount);
            paymentTransactionLogService.logFailure(PaymentConstants.METHOD_VIETQR,
                    PaymentConstants.EVENT_VIETQR_WEBHOOK_SYNC, orderId, null, orderId, pending.getAccountId(),
                    receivedAmount, "AMOUNT_MISMATCH",
                    String.format(Locale.ROOT, "Expected %s got %s", pending.getAmount(), receivedAmount),
                    requestJson, null, 200, null);
            throw new CommonServletException("Amount mismatch");
        }
        PaymentCheckoutFlowLog.step(orderId, 4, "Xác thực webhook hợp lệ — bắt đầu tạo đơn DB");

        try {
            CheckoutPendingSnapshot snapshot = objectMapper.readValue(pending.getRequestJson(), CheckoutPendingSnapshot.class);
            OrderDetailRequest orderRequest = snapshot.getOrderDetailRequest();
            orderRequest.setPaymentMethod(PaymentConstants.METHOD_VIETQR);
            java.util.List<OrderItem> cartItems = snapshot.getCartItems();

            PaymentCheckoutFlowLog.step(orderId, 5,
                    "Tạo đơn DB — accountId=%s, cartItems=%s", pending.getAccountId(),
                    cartItems != null ? cartItems.size() : 0);

            Order order = orderService.createWithOrderCode(
                    pending.getAccountId(), cartItems, orderRequest, pending.getVietqrOrderId());
            BigDecimal grandTotal = order.getTotalAmount().add(order.getShippingFee());
            order.setPrepaid(grandTotal);
            orderRepository.save(order);

            PaymentCheckoutFlowLog.step(orderId, 6,
                    "Đơn DB đã lưu — orderId=%s, orderCode=%s, grandTotal=%s",
                    order.getId(), order.getCode(), grandTotal);

            Payment payment = new Payment();
            payment.setOrder(order);
            payment.setAmount(grandTotal.doubleValue());
            payment.setPaymentMethod(PaymentConstants.METHOD_VIETQR);
            payment.setPaymentStatus(PaymentConstants.PAYMENT_STATUS_SUCCESS);
            payment.setTransactionId(payload.getTransactionId());
            paymentRepository.save(payment);

            pending.setStatus(PaymentConstants.CHECKOUT_STATUS_PAID);
            checkoutPendingRepository.save(pending);

            PaymentCheckoutFlowLog.step(orderId, 7,
                    "Thanh toán ghi nhận — paymentId txn=%s, checkout_pending=PAID", payload.getTransactionId());

            cartService.deletedCartByAccount(pending.getAccountId());
            PaymentCheckoutFlowLog.step(orderId, 8, "Đã xóa giỏ hàng — accountId=%s", pending.getAccountId());

            pancakeSyncScheduler.scheduleSyncAfterCommit(order.getId(), order.getCode());
            orderConfirmationMailScheduler.scheduleAfterCommit(order.getId(), order.getCode());

            paymentTransactionLogService.logSuccess(PaymentConstants.METHOD_VIETQR,
                    PaymentConstants.EVENT_VIETQR_WEBHOOK_SYNC, orderId, order.getId(), order.getCode(),
                    pending.getAccountId(), payload.getTransactionId(), payload.getReferencenumber(),
                    receivedAmount, requestJson, PaymentConstants.LOG_STATUS_SUCCESS, 200, null);

            paymentTransactionLogService.logSuccess(PaymentConstants.METHOD_VIETQR,
                    PaymentConstants.EVENT_ORDER_CREATED_AFTER_PAYMENT, orderId, order.getId(), order.getCode(),
                    pending.getAccountId(), payload.getTransactionId(), null, grandTotal,
                    null, null, null, null);

            PaymentCheckoutFlowLog.step(orderId, 9,
                    "Hoàn tất xử lý webhook trong transaction — chờ commit rồi sync Pancake (orderCode=%s)",
                    order.getCode());
        } catch (Exception ex) {
            PaymentCheckoutFlowLog.step(orderId, 5, "LỖI khi tạo đơn sau thanh toán: %s", ex.getMessage());
            paymentTransactionLogService.logFailure(PaymentConstants.METHOD_VIETQR,
                    PaymentConstants.EVENT_VIETQR_WEBHOOK_SYNC, orderId, null, orderId, pending.getAccountId(),
                    receivedAmount, "CONFIRM_ERROR", ex.getMessage(), requestJson, null, 500, null);
            if (ex instanceof CommonServletException) {
                throw (CommonServletException) ex;
            }
            throw new CommonServletException(ex.getMessage());
        }
    }

    private boolean reconcileCheckoutAfterDuplicateWebhook(String orderId, CheckoutPending pending,
                                                           VietQrTransactionSyncRequest payload,
                                                           String requestJson) {
        if (pending != null && PaymentConstants.CHECKOUT_STATUS_PAID.equals(pending.getStatus())) {
            return true;
        }
        Order existingOrder = orderRepository.findFirstByCode(orderId);
        if (existingOrder != null && pending != null
                && PaymentConstants.CHECKOUT_STATUS_PENDING.equals(pending.getStatus())) {
            markCheckoutPaid(pending, existingOrder, payload);
            PaymentCheckoutFlowLog.step(orderId, 3,
                    "Webhook trùng txn nhưng checkout vẫn PENDING — đã đồng bộ PAID với đơn orderId=%s",
                    existingOrder.getId());
            return true;
        }
        return false;
    }

    private void markCheckoutPaid(CheckoutPending pending, Order order, VietQrTransactionSyncRequest payload) {
        pending.setStatus(PaymentConstants.CHECKOUT_STATUS_PAID);
        checkoutPendingRepository.save(pending);
        cartService.deletedCartByAccount(pending.getAccountId());
        pancakeSyncScheduler.scheduleSyncAfterCommit(order.getId(), order.getCode());
        PaymentCheckoutFlowLog.step(pending.getVietqrOrderId(), 7,
                "checkout_pending=PAID (khôi phục) — orderCode=%s", order.getCode());
    }

    /** VND — so sánh theo số nguyên, tránh lệch scale BigDecimal. */
    static boolean amountsMatch(BigDecimal expected, BigDecimal received) {
        if (expected == null || received == null) {
            return false;
        }
        return normalizeVndAmount(expected).compareTo(normalizeVndAmount(received)) == 0;
    }

    static BigDecimal normalizeVndAmount(BigDecimal amount) {
        return amount.setScale(0, RoundingMode.HALF_UP);
    }

    /**
     * VietQR có thể gửi {@code orderId} hoặc chỉ {@code content} dạng DH{mã đơn}.
     */
    private String resolveVietqrOrderId(VietQrTransactionSyncRequest payload) {
        if (payload == null) {
            return "";
        }
        String rawOrderId = StringUtils.trimToEmpty(payload.getOrderId());
        if (StringUtils.isNotBlank(rawOrderId)
                && checkoutPendingRepository.findFirstByVietqrOrderId(rawOrderId).isPresent()) {
            return rawOrderId;
        }
        if (StringUtils.isNotBlank(rawOrderId)
                && orderRepository.findFirstByCode(rawOrderId) != null) {
            return rawOrderId;
        }
        String fromContent = extractOrderIdFromTransferContent(payload.getContent());
        if (StringUtils.isNotBlank(fromContent)
                && checkoutPendingRepository.findFirstByVietqrOrderId(fromContent).isPresent()) {
            return fromContent;
        }
        return StringUtils.isNotBlank(rawOrderId) ? rawOrderId : fromContent;
    }

    private static String extractOrderIdFromTransferContent(String content) {
        if (StringUtils.isBlank(content)) {
            return "";
        }
        String normalized = content.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("DH")) {
            return normalized.substring(2);
        }
        return normalized;
    }

    private static BigDecimal toBigDecimal(Long amount) {
        if (Objects.isNull(amount)) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(amount);
    }
}
