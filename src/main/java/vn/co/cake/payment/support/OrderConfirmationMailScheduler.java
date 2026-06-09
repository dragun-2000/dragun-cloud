package vn.co.cake.payment.support;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.service.mail.OrderConfirmationMailService;

/**
 * Gửi email xác nhận đơn hàng sau khi transaction tạo đơn đã commit.
 */
@Slf4j
@Component
public class OrderConfirmationMailScheduler {

    private final OrderConfirmationMailService orderConfirmationMailService;

    public OrderConfirmationMailScheduler(OrderConfirmationMailService orderConfirmationMailService) {
        this.orderConfirmationMailService = orderConfirmationMailService;
    }

    public void scheduleAfterCommit(Long orderId, String traceId) {
        if (orderId == null) {
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            PaymentCheckoutFlowLog.step(traceId, 9,
                    "Gửi email xác nhận đơn (không có TX) — orderId=%s", orderId);
            orderConfirmationMailService.sendForOrderAsync(orderId);
            return;
        }
        PaymentCheckoutFlowLog.step(traceId, 9,
                "Đã lên lịch gửi email xác nhận đơn sau commit (orderId=%s)", orderId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PaymentCheckoutFlowLog.step(traceId, 10,
                        "DB commit xong — gửi email xác nhận đơn (orderId=%s)", orderId);
                orderConfirmationMailService.sendForOrderAsync(orderId);
            }
        });
    }
}
