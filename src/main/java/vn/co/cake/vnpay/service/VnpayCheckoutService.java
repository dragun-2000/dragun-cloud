package vn.co.cake.vnpay.service;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpSession;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.dto.CartForm;
import vn.co.cake.dto.OrderItem;
import vn.co.cake.entity.Order;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.exception.CommonServletException;
import vn.co.cake.payment.service.InventoryReservationService;
import vn.co.cake.payment.service.PaymentTransactionLogService;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.request.OrderDetailRequest;
import vn.co.cake.service.OrderService;
import vn.co.cake.vnpay.VnpayConstants;
import vn.co.cake.vnpay.client.VnpayApiClient;
import vn.co.cake.vnpay.config.VnpayProperties;
import vn.co.cake.vnpay.dto.VnpayCheckoutResponse;
import vn.co.cake.vnpay.dto.VnpayStatusResponse;
import vn.co.cake.vnpay.entity.VnpayPayment;
import vn.co.cake.vnpay.repository.VnpayPaymentRepository;
import vn.co.cake.vnpay.support.VnpayFormats;

/**
 * Bắt đầu thanh toán VNPAY: tạo đơn AWAITING_PAYMENT + giữ kho + tạo URL redirect có checksum.
 * Kết quả thanh toán chỉ được cập nhật qua IPN ({@link VnpayIpnService}) hoặc QueryDR ({@link VnpayExpireService}).
 */
@Slf4j
@Service
public class VnpayCheckoutService {

    /** Giữ kho lâu hơn hạn VNPAY để Job004 (VietQR) không huỷ đơn VNPAY trước khi VnpayExpireJob kịp QueryDR. */
    private static final int RESERVATION_EXTRA_MINUTES = 60;

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final InventoryReservationService inventoryReservationService;
    private final PaymentTransactionLogService paymentTransactionLogService;
    private final VnpayPaymentRepository vnpayPaymentRepository;
    private final VnpayApiClient vnpayApiClient;
    private final VnpayProperties properties;
    private final VnpaySettingsService vnpaySettingsService;
    private final VnpayPilotAccessService vnpayPilotAccessService;

    public VnpayCheckoutService(OrderService orderService,
                                OrderRepository orderRepository,
                                InventoryReservationService inventoryReservationService,
                                PaymentTransactionLogService paymentTransactionLogService,
                                VnpayPaymentRepository vnpayPaymentRepository,
                                VnpayApiClient vnpayApiClient,
                                VnpayProperties properties,
                                VnpaySettingsService vnpaySettingsService,
                                VnpayPilotAccessService vnpayPilotAccessService) {
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.paymentTransactionLogService = paymentTransactionLogService;
        this.vnpayPaymentRepository = vnpayPaymentRepository;
        this.vnpayApiClient = vnpayApiClient;
        this.properties = properties;
        this.vnpaySettingsService = vnpaySettingsService;
        this.vnpayPilotAccessService = vnpayPilotAccessService;
    }

    @Transactional(rollbackFor = Exception.class)
    public VnpayCheckoutResponse startCheckout(Long accountId, CartForm cartForm, OrderDetailRequest request,
                                               String clientIp, HttpSession session) throws CommonServletException {
        if (!properties.isUsable() || !vnpaySettingsService.isFeatureEnabled()) {
            throw new CommonServletException("Thanh toán VNPAY hiện chưa khả dụng.");
        }
        vnpayPilotAccessService.assertCanUseVnpay(session, accountId);
        List<OrderItem> cartItems = cartForm.getOrderItems();
        if (CollectionUtils.isEmpty(cartItems)) {
            throw new CommonServletException("Giỏ hàng trống");
        }

        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + TimeUnit.MINUTES.toMillis(properties.getExpireMinutes()));
        Date reservationExpiresAt = new Date(expiresAt.getTime() + TimeUnit.MINUTES.toMillis(
                properties.getExpireGraceMinutes() + RESERVATION_EXTRA_MINUTES));

        String txnRef = orderService.generateOrderCode();
        request.setPaymentMethod(VnpayConstants.METHOD_VNPAY);
        Order order = orderService.createWithOrderCode(accountId, cartItems, request, txnRef);
        order.setStatus(OrderStatus.AWAITING_PAYMENT.getValue());
        order.setPrepaid(BigDecimal.ZERO);
        orderRepository.save(order);
        inventoryReservationService.reserve(order, reservationExpiresAt);

        BigDecimal amount = grandTotal(order);
        if (amount.signum() <= 0) {
            throw new CommonServletException("Số tiền thanh toán không hợp lệ");
        }

        VnpayPayment payment = new VnpayPayment();
        payment.setTxnRef(txnRef);
        payment.setOrderId(order.getId());
        payment.setAccountId(accountId);
        payment.setAmount(amount);
        payment.setStatus(VnpayConstants.STATUS_PENDING);
        payment.setOrderInfo(VnpayFormats.toOrderInfo("Thanh toan don hang " + txnRef));
        payment.setClientIp(clientIp);
        payment.setVnpCreateDate(VnpayFormats.formatDate(now));
        payment.setExpiresAt(expiresAt);
        vnpayPaymentRepository.save(payment);

        String paymentUrl = vnpayApiClient.buildPaymentUrl(payment, now, properties.getLocale());

        paymentTransactionLogService.logInfo(VnpayConstants.METHOD_VNPAY, VnpayConstants.EVENT_CHECKOUT_START,
                txnRef, accountId, amount, null, null, null, null);
        log.info("VNPAY checkout start tmn_code={} txnRef={} orderId={} accountId={} amount={} expiresAt={} paymentUrl={} temp={}",
                properties.getTmnCode(), txnRef, order.getId(), accountId, amount, expiresAt, paymentUrl, properties.getHashSecret());

        VnpayCheckoutResponse response = new VnpayCheckoutResponse();
        response.setTxnRef(txnRef);
        response.setPaymentUrl(paymentUrl);
        response.setExpiresAt(expiresAt.getTime());
        return response;
    }

    @Transactional(readOnly = true)
    public VnpayStatusResponse getStatus(String txnRef, Long accountId) throws CommonServletException {
        VnpayPayment payment = vnpayPaymentRepository.findFirstByTxnRef(txnRef)
                .filter(row -> accountId != null && row.getAccountId() == accountId)
                .orElseThrow(() -> new CommonServletException("Không tìm thấy giao dịch VNPAY"));
        VnpayStatusResponse response = new VnpayStatusResponse();
        response.setTxnRef(payment.getTxnRef());
        response.setStatus(payment.getStatus());
        response.setAmount(payment.getAmount());
        response.setResponseCode(payment.getVnpResponseCode());
        response.setMessage(VnpayResponseMessages.forStatus(payment.getStatus(), payment.getVnpResponseCode()));
        return response;
    }

    static BigDecimal grandTotal(Order order) {
        BigDecimal total = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal shipping = order.getShippingFee() != null ? order.getShippingFee() : BigDecimal.ZERO;
        return total.add(shipping);
    }
}
