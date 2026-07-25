package vn.co.cake.job;

import java.util.Date;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.entity.Order;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.payment.repository.InventoryReservationRepository;
import vn.co.cake.repository.OrderRepository;

@Component
@Slf4j
public class Job004 {

    private final InventoryReservationRepository reservationRepository;
    private final OrderRepository orderRepository;

    public Job004(InventoryReservationRepository reservationRepository,
                  OrderRepository orderRepository) {
        this.reservationRepository = reservationRepository;
        this.orderRepository = orderRepository;
    }

    /** Thu hồi giữ kho của phiên VietQR hết hạn mỗi phút. */
    @Scheduled(cron = "0 * * * * ?")
    @Transactional
    public void releaseExpiredVietQrReservations() {
        List<Long> orderIds = reservationRepository.findExpiredHeldOrderIds(new Date());
        for (Long orderId : orderIds) {
            reservationRepository.releaseHeldByOrderId(orderId);
            Order order = orderRepository.findById(orderId).orElse(null);
            if (order != null && OrderStatus.AWAITING_PAYMENT.getValue().equals(order.getStatus())) {
                order.setStatus(OrderStatus.CANCELLED.getValue());
                order.setMessageError("Phiên thanh toán VietQR đã hết hạn");
                orderRepository.save(order);
            }
        }
        if (!orderIds.isEmpty()) {
            log.info("Job004: released {} expired VietQR reservation(s)", orderIds.size());
        }
    }
}
