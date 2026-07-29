package vn.co.cake.job;

import java.util.Date;
import java.util.List;

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
 * Mỗi phút: phiên VietQR quá 15 phút (expires_at) chưa thanh toán
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
        List<Long> orderIds = reservationRepository.findExpiredHeldOrderIds(new Date());
        for (Long orderId : orderIds) {
            inventoryReservationService.release(orderId);

            Order order = orderRepository.findById(orderId).orElse(null);
            if (order == null) {
                continue;
            }

            if (OrderStatus.AWAITING_PAYMENT.getValue().equals(order.getStatus())) {
                order.setStatus(OrderStatus.CANCELLED.getValue());
                order.setMessageError("Phiên thanh toán VietQR đã hết hạn (15 phút)");
                orderRepository.save(order);
            }

            expireCheckoutPending(order.getCode());
            PaymentCheckoutFlowLog.step(order.getCode(), 10,
                    "Hết hạn 15 phút — CANCELLED + hoàn kho (orderId=%s)", orderId);
        }
        if (!orderIds.isEmpty()) {
            log.info("Job004: expired {} VietQR session(s) — CANCELLED + stock restored", orderIds.size());
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
