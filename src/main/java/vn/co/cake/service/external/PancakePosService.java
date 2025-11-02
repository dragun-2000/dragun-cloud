package vn.co.cake.service.external;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import vn.co.cake.controller.external.dto.MainOrderRequest;
import vn.co.cake.controller.external.dto.OrderPancakeResponse;
import vn.co.cake.controller.external.dto.ProductPancakeRequest;
import vn.co.cake.controller.external.dto.VariationResponse;
import vn.co.cake.dto.UpdateStockResponse;
import vn.co.cake.entity.*;
import vn.co.cake.repository.*;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class PancakePosService {

    @Value("${pancake.pos.api.warehouse-id}")
    private String pancakeApiWarehouseId;

    @Value("${pancake.pos.api.shop-id}")
    private String pancakeApiShopId;

    @Value("${pancake.pos.api.url}")
    private String pancakeApiUrl;

    private final RestTemplate restTemplate;
    private final VoucherRepository voucherRepository;
    private final PancakePropertyRepository pancakePropertyRepository;
    private final WebhookHistoryRepository webhookHistoryRepository;
    private final ProductRepository productRepository;
    private final VariationRepository variationRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderRepository orderRepository;

    public PancakePosService(RestTemplateBuilder restTemplateBuilder,
                             VoucherRepository voucherRepository,
                             PancakePropertyRepository pancakePropertyRepository,
                             WebhookHistoryRepository webhookHistoryRepository,
                             ProductRepository productRepository,
                             VariationRepository variationRepository,
                             WarehouseRepository warehouseRepository,
                             OrderRepository orderRepository) {
        this.restTemplate = restTemplateBuilder.build();
        this.voucherRepository = voucherRepository;
        this.pancakePropertyRepository = pancakePropertyRepository;
        this.webhookHistoryRepository = webhookHistoryRepository;
        this.productRepository = productRepository;
        this.variationRepository = variationRepository;
        this.warehouseRepository = warehouseRepository;
        this.orderRepository = orderRepository;
    }

    public void saveWebhookHistory(String payload) {
        WebhookHistory webhookHistory = new WebhookHistory();
        webhookHistory.setPayload(payload);
        this.updateStock(payload, webhookHistory);
    }

    private void updateStock(String payload, WebhookHistory webhookHistory) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            UpdateStockResponse data = objectMapper.readValue(payload, UpdateStockResponse.class);
            Variation variation = variationRepository.findFirstByVariationId(data.getVariationId());
            if (Objects.isNull(variation)) return;
            variation.setRemainQuantity(data.getRemainQuantity());
            variationRepository.save(variation);
            List<Variation> variations = variationRepository.findAllByPancakeProductId(variation.getPancakeProductId());
            long totalStock = 0;
            for (Variation variationLinked : variations) {
                if (variationLinked.getId() == variation.getId()) {
                    totalStock += data.getRemainQuantity();
                } else {
                    totalStock += variationLinked.getRemainQuantity();
                }
            }
            
            Product product = productRepository.findFirstByProductPancakeId(variation.getPancakeProductId());
            product.setStockQuantity(totalStock);
            productRepository.save(product);

//            webhookHistoryRepository.save(webhookHistory);
        } catch (Exception e) {
//            log.error("Case not matching data = {}", e.getMessage());
        }
    }

    public List<VariationResponse> createProduct(ProductPancakeRequest productPancakeRequest) {
        try {
            PancakeProperties pancakeProperty = getDefault();
            String url = pancakeApiUrl + "/shops/" + pancakeProperty.getShopId() + "/products?api_key=" + pancakeProperty.getToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            HttpEntity<ProductPancakeRequest> request = new HttpEntity<>(productPancakeRequest, headers);

            String jsonString = objectMapper.writeValueAsString(productPancakeRequest);
            System.out.println("\n" + jsonString + "\n");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String variationJson = rootNode.path("data").path("variations").toString();
                return objectMapper.readValue(variationJson, objectMapper.getTypeFactory().constructCollectionType(List.class, VariationResponse.class));
            } else {
                log.error("Failed to sync order with Pancake POS. Response: {}", response.getBody());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error occurred while syncing order: {}", e.getMessage());
            return null;
        }
    }

    public boolean createOrder(Order order) {
        try {
            PancakeProperties pancakeProperty = this.getDefault();
            Warehouse warehouse = this.getWarehouseDefault();
            String url = pancakeApiUrl + "/shops/" + pancakeProperty.getShopId() + "/orders?api_key=" + pancakeProperty.getToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            MainOrderRequest mainOrderRequest = new MainOrderRequest(order, pancakeProperty, warehouse);
            HttpEntity<MainOrderRequest> request = new HttpEntity<>(mainOrderRequest, headers);

            String jsonString = objectMapper.writeValueAsString(mainOrderRequest);
            System.out.println("\n" + jsonString + "\n");
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            if (response.getStatusCode() == HttpStatus.CREATED) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                System.out.println(rootNode);
                return true;
            } else {
                log.error("Failed to sync order with Pancake POS. Response: {}", response.getBody());
                return false;
            }
        } catch (Exception e) {
            order.setDeleted(true);
            orderRepository.save(order);
            e.printStackTrace();
            log.error("Error occurred while syncing order: {}", e.getMessage());
            return false;
        }
    }
    
    public List<VariationResponse> getAllProductPancake(int pageNumber, int pageSize) {
        try {
            PancakeProperties pancakeProperty = getDefault();
            String url = pancakeApiUrl + "/shops/" + pancakeProperty.getShopId() + "/products/variations?api_key=" + pancakeProperty.getToken() + "&page_number=" + pageNumber;
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String variationJson = rootNode.path("data").toString();
                return objectMapper.readValue(variationJson, objectMapper.getTypeFactory().constructCollectionType(List.class, VariationResponse.class));
            } else {
                log.error("Failed to sync Product with Pancake POS. Response: {}", response.getBody());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error occurred while syncing Product: {}", e.getMessage());
            return null;
        }
    }

    public List<OrderPancakeResponse> getAllOrderPancake(String phone, int pageNumber, int pageSize) {
        try {
            PancakeProperties pancakeProperty = getDefault();
            String url = pancakeApiUrl + "/shops/" + pancakeProperty.getShopId() + "/orders?api_key=" + pancakeProperty.getToken() + "&page_size=" + pageSize + "&page_number=" + pageNumber + "&search=" + phone;
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String variationJson = rootNode.path("data").toString();
                return objectMapper.readValue(variationJson, objectMapper.getTypeFactory().constructCollectionType(List.class, OrderPancakeResponse.class));
            } else {
                log.error("Failed to sync Order with Pancake POS. Response: {}", response.getBody());
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error occurred while syncing Order: {}", e.getMessage());
            return null;
        }
    }

    private PancakeProperties getDefault() {
        return pancakePropertyRepository.findFirstByDeletedIsFalse();
    }

    private Warehouse getWarehouseDefault() {
        return warehouseRepository.findFirstByDeletedIsFalse();
    }
}
