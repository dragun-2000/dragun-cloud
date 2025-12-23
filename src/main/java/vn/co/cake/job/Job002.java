package vn.co.cake.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.co.cake.constants.OrderConstants;
import vn.co.cake.entity.Order;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.repository.OrderRepository;
import vn.co.cake.service.external.PancakePosService;

import java.util.List;

@Component
@Slf4j
public class Job002 {
    
    private final OrderRepository orderRepository;
    private final PancakePosService pancakePosService;
    
    public Job002(OrderRepository orderRepository, PancakePosService pancakePosService) {
        this.orderRepository = orderRepository;
        this.pancakePosService = pancakePosService;
    }
    
    @Scheduled(cron = "0 */30 * * * ?") // Chạy mỗi 30 phút
    public void retryFailedOrders() {
        log.info("Job002: Starting retry failed orders sync to Pancake POS");

        List<Order> failedOrders = orderRepository.findByStatusAndCountErrorLessThan(
                OrderStatus.SYNC_FAIL.getValue(),
                OrderConstants.MAX_PANCAKE_SYNC_RETRIES
        );

        if (failedOrders.isEmpty()) {
            log.info("Job002: No failed orders to retry");
            return;
        }

        log.info("Job002: Found {} failed orders to retry", failedOrders.size());

        for (Order order : failedOrders) {
            try {
                log.info("Job002: Found 1 {} failed getOrderItems to retry", order.getOrderItems().size());
                boolean success = pancakePosService.createOrder(order);
                if (!success) {
                    log.info("Job002: Fail retry");
                    return;
                }
            } catch (Exception e) {
                String orderCode = order.getCode() != null ? order.getCode() : "ID:" + order.getId();
                log.error("Job002: Failed to sync order {} to Pancake POS. Status: {}, CountError: {}, Message: {} (will retry next time)",
                        orderCode,
                        order.getStatus(),
                        order.getCountError(),
                        e.getMessage(), e);
            }
            log.info("Job002: Completed retry");
        }
    }
}

