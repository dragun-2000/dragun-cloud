package vn.co.cake.payment.support;

import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import vn.co.cake.entity.Order;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.service.external.PancakePosService;

/**
 * Đảm bảo sync Pancake chỉ chạy sau khi transaction tạo đơn đã commit.
 */
@Component
public class PancakeSyncScheduler {

    private final OrderRepository orderRepository;
    private final PancakePosService pancakePosService;

    public PancakeSyncScheduler(OrderRepository orderRepository, PancakePosService pancakePosService) {
        this.orderRepository = orderRepository;
        this.pancakePosService = pancakePosService;
    }

    public void scheduleSyncAfterCommit(Long orderId, String traceId) {
        if (orderId == null) {
            PaymentCheckoutFlowLog.step(traceId, 9, "Bỏ qua sync Pancake: orderId null");
            return;
        }
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            PaymentCheckoutFlowLog.step(traceId, 9,
                    "Không có transaction đang mở — gọi sync Pancake trực tiếp (orderId=%s)", orderId);
            triggerSync(orderId, traceId);
            return;
        }
        PaymentCheckoutFlowLog.step(traceId, 9,
                "Đã lên lịch sync Pancake POS sau khi commit DB (orderId=%s)", orderId);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                PaymentCheckoutFlowLog.step(traceId, 10, "DB commit xong — kích hoạt sync Pancake POS (orderId=%s)", orderId);
                triggerSync(orderId, traceId);
            }

            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    PaymentCheckoutFlowLog.step(traceId, 10,
                            "Transaction rollback — không sync Pancake (orderId=%s)", orderId);
                }
            }
        });
    }

    private void triggerSync(Long orderId, String traceId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            PaymentCheckoutFlowLog.step(traceId, 10,
                    "LỖI: không tìm thấy đơn sau commit (orderId=%s) — không sync Pancake", orderId);
            return;
        }
        pancakePosService.syncOrderToPancakeAsync(order, traceId);
    }
}
