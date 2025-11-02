package vn.co.cake.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import vn.co.cake.controller.external.dto.VariationResponse;
import vn.co.cake.entity.Voucher;
import vn.co.cake.repository.VoucherRepository;
import vn.co.cake.service.ProductService;
import vn.co.cake.service.external.PancakePosService;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Slf4j
public class Job01 {

    private final ProductService productService;
    private final VoucherRepository voucherRepository;
    private final PancakePosService pancakePosService;

    public Job01(ProductService productService, VoucherRepository voucherRepository, PancakePosService pancakePosService) {
        this.productService = productService;
        this.voucherRepository = voucherRepository;
        this.pancakePosService = pancakePosService;
    }

   @Scheduled(cron = "0 * * * * ?")
    public void updateDiscountIfNeeded() {
        LocalDate currentDate = LocalDate.now();
        
        Voucher voucher = voucherRepository.findFirstByCodeAndDeletedIsFalse("DISCOUNT_PRICE");
        if (voucher == null) {
            return;
        }
        log.info(">> updateDiscountIfNeeded voucher = {} <<", voucher.getId());

        LocalDate startDate = voucher.getStartDate();
        LocalDate endDate = voucher.getEndDate();
        if (startDate == null || endDate == null) return;

        // Kiểm tra nếu ngày hiện tại nằm ngoài khoảng thời gian discount
        if (currentDate.isBefore(startDate) || currentDate.isAfter(endDate)) {
            productService.resetProductDiscountPrice();
        }
    }
    
   @Scheduled(cron = "0 0 5 * * ?")
    public void syncPancakeData() {
        log.info("*** Start syncProducts ***");
        int pageNumber = 0;
        int pageSize = 30;
        int responseSize;
        Set<VariationResponse> variationPancakes = new HashSet<>();
        do {
            List<VariationResponse> variationResponses = pancakePosService.getAllProductPancake(pageNumber, pageSize);
            responseSize = variationResponses.size();
            if (variationResponses.isEmpty()) break;
            
            variationPancakes.addAll(variationResponses);
            log.info("\n pageNumber = {}", pageNumber);
            pageNumber++;
        } while (pageSize == responseSize);
        productService.syncProductPancake(variationPancakes);
        log.info("*** End syncProducts ***");
    }
}
