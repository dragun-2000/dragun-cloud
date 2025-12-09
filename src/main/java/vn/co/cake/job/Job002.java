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
        
        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;
        
        for (Order order : failedOrders) {
            try {
                // Reload order từ DB để đảm bảo có data mới nhất, tránh duplicate
                Order freshOrder = orderRepository.findById(order.getId()).orElse(null);
                if (freshOrder == null) {
                    String orderCode = order.getCode() != null ? order.getCode() : "ID:" + order.getId();
                    log.error("Job002: Order {} not found in database, skipping", orderCode);
                    skippedCount++;
                    continue;
                }
                
                // Double check: chỉ retry nếu status vẫn là SYNC_FAIL
                if (!OrderStatus.SYNC_FAIL.getValue().equals(freshOrder.getStatus())) {
                    log.info("Job002: Order {} status changed to {}, skipping retry", 
                        freshOrder.getCode(), freshOrder.getStatus());
                    skippedCount++;
                    continue;
                }
                
                // Check countError để tránh retry quá nhiều lần
                if (freshOrder.getCountError() != null && 
                    freshOrder.getCountError() >= OrderConstants.MAX_PANCAKE_SYNC_RETRIES) {
                    log.error("Job002: Order {} has reached max retry count ({}), skipping", 
                        freshOrder.getCode(), freshOrder.getCountError());
                    skippedCount++;
                    continue;
                }
                
                log.info("Job002: Retrying sync for order {} (attempt {}/{})", 
                    freshOrder.getCode(), 
                    (freshOrder.getCountError() != null ? freshOrder.getCountError() + 1 : 1),
                    OrderConstants.MAX_PANCAKE_SYNC_RETRIES);
                
                // Call createOrder - method này sẽ update status tự động
                boolean success = pancakePosService.createOrder(freshOrder);
                
                if (success) {
                    successCount++;
                    log.info("Job002: Successfully synced order {} to Pancake POS", freshOrder.getCode());
                } else {
                    failCount++;
                    // Reload lại để xem status đã được update chưa
                    Order updatedOrder = orderRepository.findById(freshOrder.getId()).orElse(null);
                    if (updatedOrder != null) {
                        log.error("Job002: Failed to sync order {} to Pancake POS. Status: {}, CountError: {}, Message: {} (will retry next time)", 
                            updatedOrder.getCode(), 
                            updatedOrder.getStatus(),
                            updatedOrder.getCountError(),
                            updatedOrder.getMessageError());
                    } else {
                        log.error("Job002: Failed to sync order {} to Pancake POS (will retry next time)", freshOrder.getCode());
                    }
                }
            } catch (Exception e) {
                failCount++;
                String orderCode = order.getCode() != null ? order.getCode() : "ID:" + order.getId();
                log.error("Job002: Error while retrying order {}: {}", orderCode, e.getMessage(), e);
            }
        }
        
        log.info("Job002: Completed retry. Total: {}, Success: {}, Failed: {}, Skipped: {}", 
            failedOrders.size(), successCount, failCount, skippedCount);
    }
}

