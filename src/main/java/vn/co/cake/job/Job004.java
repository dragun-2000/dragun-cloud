package vn.co.cake.job;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.entity.Order;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.payment.PaymentConstants;
import vn.co.cake.payment.entity.CheckoutPending;
import vn.co.cake.payment.repository.CheckoutPendingRepository;
import vn.co.cake.payment.repository.InventoryReservationRepository;
import vn.co.cake.payment.service.InventoryReservationService;
import vn.co.cake.payment.support.PaymentCheckoutFlowLog;
import vn.co.cake.repository.OrderRepository;

/**
 * Mỗi phút: phiên VietQR quá hạn (expires_at, mặc định 5 phút) chưa thanh toán
 * → hoàn kho + CANCELLED đơn + EXPIRED checkout_pending.
 */
@Component
@Slf4j
public class Job004 {

    private final InventoryReservationRepository reservationRepository;
    private final InventoryReservationService inventoryReservationService;
    private final OrderRepository orderRepository;
    private final CheckoutPendingRepository checkoutPendingRepository;

    public Job004(InventoryReservationRepository reservationRepository,
                  InventoryReservationService inventoryReservationService,
                  OrderRepository orderRepository,
                  CheckoutPendingRepository checkoutPendingRepository) {
        this.reservationRepository = reservationRepository;
        this.inventoryReservationService = inventoryReservationService;
        this.orderRepository = orderRepository;
        this.checkoutPendingRepository = checkoutPendingRepository;
    }

    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void releaseExpiredVietQrReservations() {
        Date now = new Date();
        Set<Long> orderIds = new LinkedHashSet<>(reservationRepository.findExpiredHeldOrderIds(now));

        for (CheckoutPending pending : checkoutPendingRepository.findExpiredPending(now)) {
            Order order = orderRepository.findFirstByCode(pending.getVietqrOrderId());
            if (order != null) {
                orderIds.add(order.getId());
            } else if (PaymentConstants.CHECKOUT_STATUS_PENDING.equals(pending.getStatus())) {
                pending.setStatus(PaymentConstants.CHECKOUT_STATUS_EXPIRED);
                checkoutPendingRepository.save(pending);
            }
        }

        int expiredCount = 0;
        for (Long orderId : orderIds) {
            if (expireUnpaidOrder(orderId)) {
                expiredCount++;
            }
        }
        if (expiredCount > 0) {
            log.info("Job004: expired {} VietQR session(s) — CANCELLED + stock restored", expiredCount);
        }
    }

    /**
     * @return true nếu đã hết hạn phiên chưa thanh toán (CANCELLED/hoàn kho)
     */
    private boolean expireUnpaidOrder(Long orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return false;
        }

        // Đơn đã thanh toán / đang xử lý — không EXPIRED pending, không đụng kho CONFIRMED.
        if (!OrderStatus.AWAITING_PAYMENT.getValue().equals(order.getStatus())
                && !OrderStatus.CANCELLED.getValue().equals(order.getStatus())) {
            markPendingPaidIfStillPending(order.getCode());
            return false;
        }

        inventoryReservationService.release(orderId);

        if (OrderStatus.AWAITING_PAYMENT.getValue().equals(order.getStatus())) {
            order.setStatus(OrderStatus.CANCELLED.getValue());
            order.setMessageError("Phiên thanh toán VietQR đã hết hạn (5 phút)");
            orderRepository.save(order);
        }

        expireCheckoutPending(order.getCode());
        PaymentCheckoutFlowLog.step(order.getCode(), 10,
                "Hết hạn thanh toán VietQR — CANCELLED + hoàn kho (orderId=%s)", orderId);
        return true;
    }

    private void markPendingPaidIfStillPending(String vietqrOrderId) {
        if (vietqrOrderId == null) {
            return;
        }
        CheckoutPending pending = checkoutPendingRepository.findFirstByVietqrOrderId(vietqrOrderId).orElse(null);
        if (pending != null && PaymentConstants.CHECKOUT_STATUS_PENDING.equals(pending.getStatus())) {
            pending.setStatus(PaymentConstants.CHECKOUT_STATUS_PAID);
            checkoutPendingRepository.save(pending);
        }
    }

    private void expireCheckoutPending(String vietqrOrderId) {
        if (vietqrOrderId == null) {
            return;
        }
        CheckoutPending pending = checkoutPendingRepository.findFirstByVietqrOrderId(vietqrOrderId).orElse(null);
        if (pending != null && PaymentConstants.CHECKOUT_STATUS_PENDING.equals(pending.getStatus())) {
            pending.setStatus(PaymentConstants.CHECKOUT_STATUS_EXPIRED);
            checkoutPendingRepository.save(pending);
        }
    }
}
