package vn.co.cake.job;

import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.extern.slf4j.Slf4j;
import vn.co.cake.service.external.PancakePosService;

/**
 * Đồng bộ trạng thái / tracking từ Pancake POS vào bảng orders mỗi giờ
 * (màn /order-history chỉ đọc DB, không gọi Pancake).
 * Bỏ qua đơn đã có pancake_status_name = shipped hoặc canceled.
 */
@Component
@Slf4j
public class Job003 {

    private static final int BATCH_SIZE = 200;

    private final PancakePosService pancakePosService;

    public Job003(PancakePosService pancakePosService) {
        this.pancakePosService = pancakePosService;
    }

    @Scheduled(cron = "0 0 * * * ?")
    public void syncOrdersFromPancake() {
        log.info("Job003: Bắt đầu đồng bộ Pancake (tối đa {} đơn/lần)", BATCH_SIZE);
        try {
            pancakePosService.syncOrdersFromPancakeHourly(BATCH_SIZE);
        } catch (Exception e) {
            log.error("Job003: Lỗi đồng bộ Pancake: {}", e.getMessage(), e);
        }
        log.info("Job003: Hoàn tất đồng bộ Pancake");
    }
}
