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
    
    @Scheduled(cron = "0 */30 * * * ?") // Chạy mỗi 10 phút
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
        
        int successCount = 0;
        int failCount = 0;
        
        for (Order order : failedOrders) {
            try {
                boolean success = pancakePosService.createOrder(order);
                if (success) {
                    successCount++;
                    log.info("Job002: Successfully synced order {} to Pancake POS", order.getCode());
                } else {
                    failCount++;
                    log.error("Job002: Failed to sync order {} to Pancake POS (will retry next time)", order.getCode());
                }
            } catch (Exception e) {
                failCount++;
                log.error("Job002: Error while retrying order {}: {}", order.getCode(), e.getMessage(), e);
            }
        }
        
        log.info("Job002: Completed retry. Total: {}, Success: {}, Failed: {}", 
            failedOrders.size(), successCount, failCount);
    }
}

