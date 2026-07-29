package vn.co.cake.payment.service;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.config.VietQrProperties;
import vn.co.cake.payment.dto.CheckoutPendingSnapshot;
import vn.co.cake.payment.dto.PaymentCheckoutResponse;
import vn.co.cake.payment.dto.PaymentCheckoutStatusResponse;
import vn.co.cake.payment.dto.VietQrGenerateResponse;
import vn.co.cake.payment.dto.VietQrTransactionSyncRequest;
import vn.co.cake.entity.Order;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.payment.entity.CheckoutPending;
import vn.co.cake.payment.repository.CheckoutPendingRepository;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.payment.util.VietQrContentNormalizer;
import vn.co.cake.payment.vietqr.VietQrApiClient;
import vn.co.cake.request.OrderDetailRequest;
import vn.co.cake.payment.support.PaymentCheckoutFlowLog;
import vn.co.cake.payment.support.VietQrPaymentUrlResolver;
import vn.co.cake.service.OrderService;

@Service
public class VietQrCheckoutService {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final CheckoutPendingRepository checkoutPendingRepository;
    private final VietQrApiClient vietQrApiClient;
    private final VietQrProperties vietQrProperties;
    private final PaymentTransactionLogService paymentTransactionLogService;
    private final VietQrWebhookService vietQrWebhookService;
    private final InventoryReservationService inventoryReservationService;
    private final ObjectMapper objectMapper;

    public VietQrCheckoutService(OrderService orderService,
                                 OrderRepository orderRepository,
                                 CheckoutPendingRepository checkoutPendingRepository,
                                 VietQrApiClient vietQrApiClient,
                                 VietQrProperties vietQrProperties,
                                 PaymentTransactionLogService paymentTransactionLogService,
                                 VietQrWebhookService vietQrWebhookService,
                                 InventoryReservationService inventoryReservationService,
                                 ObjectMapper objectMapper) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.checkoutPendingRepository = checkoutPendingRepository;
        this.vietQrApiClient = vietQrApiClient;
        this.vietQrProperties = vietQrProperties;
        this.paymentTransactionLogService = paymentTransactionLogService;
        this.vietQrWebhookService = vietQrWebhookService;
        this.inventoryReservationService = inventoryReservationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentCheckoutResponse startCheckout(Long accountId, CartForm cartForm, OrderDetailRequest request)
            throws Exception {
        if (!vietQrProperties.isEnabled()) {
            throw new CommonServletException("VietQR payment is not enabled");
        }
        List<OrderItem> cartItems = cartForm.getOrderItems();
        if (CollectionUtils.isEmpty(cartItems)) {
            throw new CommonServletException("Giỏ hàng trống");
        }
        String vietqrOrderId = orderService.generateOrderCode();
        BigDecimal grandTotal = orderService.calculateGrandTotal(cartItems);
        String content = VietQrContentNormalizer.normalizeOrderContent(vietqrOrderId);
        Date expiresAt = calculateExpiresAt();

        CheckoutPendingSnapshot snapshot = new CheckoutPendingSnapshot();
        request.setPaymentMethod(PaymentConstants.METHOD_VIETQR);
        snapshot.setOrderDetailRequest(request);
        snapshot.setCartItems(cartItems);

        Order draftOrder = orderService.createWithOrderCode(accountId, cartItems, request, vietqrOrderId);
        draftOrder.setStatus(OrderStatus.AWAITING_PAYMENT.getValue());
        draftOrder.setPrepaid(BigDecimal.ZERO);
        orderRepository.save(draftOrder);
        inventoryReservationService.reserve(draftOrder, expiresAt);
        PaymentCheckoutFlowLog.step(vietqrOrderId, 1,
                "Đã trừ kho (HELD) khi submit VietQR — orderId=%s, expiresAt=%s",
                draftOrder.getId(), expiresAt);

        CheckoutPending pending = new CheckoutPending();
        pending.setVietqrOrderId(vietqrOrderId);
        pending.setAccountId(accountId);
        pending.setAmount(grandTotal);
        pending.setContent(content);
        pending.setStatus(PaymentConstants.CHECKOUT_STATUS_PENDING);
        pending.setRequestJson(objectMapper.writeValueAsString(snapshot));
        pending.setExpiresAt(expiresAt);
        checkoutPendingRepository.save(pending);

        PaymentCheckoutFlowLog.step(vietqrOrderId, 1,
                "Khách bắt đầu thanh toán VietQR — accountId=%s, amount=%s, content=%s",
                accountId, grandTotal, content);

        VietQrGenerateResponse qr = vietQrApiClient.generateCustomerQr(
                vietqrOrderId, grandTotal, content, PaymentConstants.METHOD_VIETQR);
        String paymentPageUrl = VietQrPaymentUrlResolver.resolve(
                qr, vietQrProperties.getCheckout().getPaymentPageBaseUrl());
        pending.setQrLink(paymentPageUrl != null ? paymentPageUrl : qr.getQrLink());
        pending.setQrCode(qr.getQrCode());
        checkoutPendingRepository.save(pending);

        paymentTransactionLogService.logInfo(PaymentConstants.METHOD_VIETQR,
                PaymentConstants.EVENT_CHECKOUT_VIETQR_START, vietqrOrderId, accountId,
                grandTotal, null, qr.getQrLink(), null, null);

        PaymentCheckoutFlowLog.step(vietqrOrderId, 1,
                "Đã tạo mã QR — chờ chuyển khoản hoặc webhook / sandbox-simulate");

        PaymentCheckoutResponse response = new PaymentCheckoutResponse();
        response.setResultType(PaymentConstants.RESULT_TYPE_QR_PAYMENT);
        response.setOrderId(vietqrOrderId);
        response.setQrCode(qr.getQrCode());
        response.setQrLink(paymentPageUrl != null ? paymentPageUrl : qr.getQrLink());
        response.setAmount(grandTotal);
        response.setContent(content);
        response.setExpiresAt(expiresAt != null ? expiresAt.getTime() : null);
        return response;
    }

    @Transactional
    public String getCheckoutStatus(String vietqrOrderId) {
        return resolveCheckoutStatus(findPendingOrNull(vietqrOrderId));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentCheckoutStatusResponse getCheckoutDetails(String vietqrOrderId, Long accountId)
            throws CommonServletException {
        String normalizedId = normalizeVietqrOrderId(vietqrOrderId);
        CheckoutPending pending = findPendingOrNull(normalizedId);
        if (pending == null) {
            Order order = orderRepository.findFirstByCode(normalizedId);
            if (order != null && OrderStatus.PAYMENT_RECEIVED_UNFULFILLABLE.getValue().equals(order.getStatus())) {
                PaymentCheckoutStatusResponse issue = buildPaidStatusResponse(normalizedId, order,
                        "Đã nhận thanh toán. Đơn hàng đang cần hỗ trợ xử lý tồn kho; "
                                + "bộ phận chăm sóc khách hàng sẽ liên hệ với bạn.");
                issue.setStatus(PaymentConstants.CHECKOUT_STATUS_PAID_ISSUE);
                return issue;
            }
            if (order != null && !OrderStatus.AWAITING_PAYMENT.getValue().equals(order.getStatus())
                    && !OrderStatus.CANCELLED.getValue().equals(order.getStatus())) {
                return buildPaidStatusResponse(normalizedId, order, "Thanh toán thành công — đơn đã được tạo");
            }
            throw new CommonServletException("Không tìm thấy phiên thanh toán");
        }
        if (accountId != null && pending.getAccountId() != accountId.longValue()) {
            throw new CommonServletException("Không có quyền xem phiên thanh toán này");
        }
        syncCheckoutPendingWithExistingOrder(pending);
        pending = findPendingOrNull(normalizedId);
        return toStatusResponse(pending);
    }

    /**
     * Mô phỏng webhook VietQR (dev). Không bọc @Transactional để confirmPayment (REQUIRES_NEW) commit
     * trước khi đọc lại trạng thái — tránh cache JPA trả PENDING sau khi DB đã PAID.
     */
    public void runSandboxPaymentConfirm(String vietqrOrderId, Long accountId) throws Exception {
        if (!vietQrProperties.getCheckout().isSandboxSimulateEnabled()) {
            throw new CommonServletException("Sandbox simulate is disabled");
        }
        CheckoutPending pending = checkoutPendingRepository.findFirstByVietqrOrderId(vietqrOrderId)
                .orElseThrow(() -> new CommonServletException("Không tìm thấy phiên thanh toán"));
        if (accountId != null && pending.getAccountId() != accountId.longValue()) {
            throw new CommonServletException("Không có quyền xác nhận phiên thanh toán này");
        }
        String resolvedStatus = resolveCheckoutStatus(pending);
        if (!PaymentConstants.CHECKOUT_STATUS_PENDING.equals(resolvedStatus)) {
            return;
        }

        PaymentCheckoutFlowLog.step(vietqrOrderId, 1,
                "Sandbox: mô phỏng webhook thanh toán thành công (dev)");

        VietQrTransactionSyncRequest payload = new VietQrTransactionSyncRequest();
        payload.setOrderId(vietqrOrderId);
        payload.setContent(pending.getContent());
        payload.setAmount(VietQrWebhookService.normalizeVndAmount(pending.getAmount()).longValue());
        payload.setTransType(PaymentConstants.VIETQR_TRANS_TYPE_CREDIT);
        payload.setTransactionId("SANDBOX-" + System.currentTimeMillis());
        payload.setBankaccount(vietQrProperties.getBank().getAccount());

        vietQrWebhookService.confirmPayment(payload, objectMapper.writeValueAsString(payload));
    }

    private CheckoutPending findPendingOrNull(String vietqrOrderId) {
        if (StringUtils.isBlank(vietqrOrderId)) {
            return null;
        }
        return checkoutPendingRepository.findFirstByVietqrOrderId(vietqrOrderId.trim()).orElse(null);
    }

    /** Đồng bộ PAID chỉ khi draft đã thực sự được thanh toán/xử lý. */
    private void syncCheckoutPendingWithExistingOrder(CheckoutPending pending) {
        if (pending == null || !PaymentConstants.CHECKOUT_STATUS_PENDING.equals(pending.getStatus())) {
            return;
        }
        Order order = orderRepository.findFirstByCode(pending.getVietqrOrderId());
        if (order != null && !OrderStatus.AWAITING_PAYMENT.getValue().equals(order.getStatus())
                && !OrderStatus.CANCELLED.getValue().equals(order.getStatus())) {
            pending.setStatus(PaymentConstants.CHECKOUT_STATUS_PAID);
            checkoutPendingRepository.save(pending);
            PaymentCheckoutFlowLog.step(pending.getVietqrOrderId(), 1,
                    "Đồng bộ checkout_pending=PAID vì đơn DB đã tồn tại — orderId=%s", order.getId());
        }
    }

    private static String normalizeVietqrOrderId(String vietqrOrderId) {
        return vietqrOrderId != null ? vietqrOrderId.trim() : "";
    }

    private PaymentCheckoutStatusResponse buildPaidStatusResponse(String vietqrOrderId, Order order, String message) {
        PaymentCheckoutStatusResponse response = new PaymentCheckoutStatusResponse();
        response.setStatus(PaymentConstants.CHECKOUT_STATUS_PAID);
        response.setOrderId(vietqrOrderId);
        response.setMessage(message);
        response.setOrderCreated(true);
        response.setAwaitingPayment(false);
        if (order.getTotalAmount() != null) {
            response.setAmount(order.getTotalAmount().add(order.getShippingFee()));
        }
        return response;
    }

    private String resolveCheckoutStatus(CheckoutPending pending) {
        if (pending == null) {
            return PaymentConstants.CHECKOUT_STATUS_EXPIRED;
        }
        if (PaymentConstants.CHECKOUT_STATUS_PENDING.equals(pending.getStatus())
                && pending.getExpiresAt() != null
                && pending.getExpiresAt().before(new Date())) {
            pending.setStatus(PaymentConstants.CHECKOUT_STATUS_EXPIRED);
            checkoutPendingRepository.save(pending);
            Order order = orderRepository.findFirstByCode(pending.getVietqrOrderId());
            if (order != null && OrderStatus.AWAITING_PAYMENT.getValue().equals(order.getStatus())) {
                inventoryReservationService.release(order.getId());
                order.setStatus(OrderStatus.CANCELLED.getValue());
                order.setMessageError("Phiên thanh toán VietQR đã hết hạn (15 phút)");
                orderRepository.save(order);
            }
        }
        return pending.getStatus();
    }

    private PaymentCheckoutStatusResponse toStatusResponse(CheckoutPending pending) {
        PaymentCheckoutStatusResponse response = new PaymentCheckoutStatusResponse();
        if (pending == null) {
            response.setStatus(PaymentConstants.CHECKOUT_STATUS_EXPIRED);
            response.setMessage("Phiên thanh toán không tồn tại hoặc đã hết hạn");
            return response;
        }
        String status = resolveCheckoutStatus(pending);
        Order order = orderRepository.findFirstByCode(pending.getVietqrOrderId());
        boolean orderCreated = order != null
                && !OrderStatus.AWAITING_PAYMENT.getValue().equals(order.getStatus())
                && !OrderStatus.CANCELLED.getValue().equals(order.getStatus());
        response.setStatus(status);
        response.setOrderId(pending.getVietqrOrderId());
        response.setAmount(pending.getAmount());
        response.setContent(pending.getContent());
        response.setQrLink(pending.getQrLink());
        response.setQrCode(pending.getQrCode());
        response.setExpiresAt(pending.getExpiresAt() != null ? pending.getExpiresAt().getTime() : null);
        response.setOrderCreated(orderCreated);
        response.setAwaitingPayment(PaymentConstants.CHECKOUT_STATUS_PENDING.equals(status) && !orderCreated);
        if (PaymentConstants.CHECKOUT_STATUS_PENDING.equals(status)) {
            response.setMessage(orderCreated
                    ? "Đang hoàn tất đơn hàng..."
                    : "Đã tạo mã QR — chưa nhận chuyển khoản. Vui lòng quét mã và CK đúng số tiền, nội dung.");
        } else if (PaymentConstants.CHECKOUT_STATUS_PAID.equals(status)) {
            response.setMessage("Thanh toán thành công");
            response.setOrderCreated(true);
            response.setAwaitingPayment(false);
        } else if (PaymentConstants.CHECKOUT_STATUS_PAID_ISSUE.equals(status)) {
            response.setMessage("Đã nhận thanh toán. Đơn hàng đang cần hỗ trợ xử lý tồn kho; "
                    + "bộ phận chăm sóc khách hàng sẽ liên hệ với bạn.");
            response.setOrderCreated(true);
            response.setAwaitingPayment(false);
        } else if (PaymentConstants.CHECKOUT_STATUS_EXPIRED.equals(status)) {
            response.setMessage("Phiên thanh toán đã hết hạn");
        }
        return response;
    }

    private Date calculateExpiresAt() {
        Calendar calendar = Calendar.getInstance();
        int expireMinutes = Math.max(vietQrProperties.getCheckout().getExpireMinutes(), 1);
        calendar.add(Calendar.MINUTE, expireMinutes);
        return calendar.getTime();
    }
}
