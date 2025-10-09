package vn.co.cake.controller.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import vn.co.cake.controller.external.dto.VariationResponse;
import vn.co.cake.service.ProductService;
import vn.co.cake.service.external.PancakePosService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/v2/pancake")
public class PancakeController {
    private final PancakePosService pancakePosService;
    private final ProductService productService;

    public PancakeController(PancakePosService pancakePosService, 
                             ProductService productService) {
        this.pancakePosService = pancakePosService;
        this.productService = productService;
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleWebhook(@RequestBody String payload) {
        log.info("handleWebhook = {}", payload);
        pancakePosService.saveWebhookHistory(payload);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/sync-products")
    public ResponseEntity<Void> syncProducts() {
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
        return ResponseEntity.ok().build();
    }
}
