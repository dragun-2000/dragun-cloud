package vn.co.cake.service.external;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.annotation.Transactional;
import vn.co.cake.controller.external.dto.MainOrderRequest;
import vn.co.cake.controller.external.dto.OrderPancakeResponse;
import vn.co.cake.controller.external.dto.ProductPancakeRequest;
import vn.co.cake.controller.external.dto.VariationResponse;
import vn.co.cake.dto.UpdateStockResponse;
import vn.co.cake.entity.*;
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.repository.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Qualifier;

@Service
@Slf4j
public class PancakePosService {

    @Value("${pancake.pos.api.warehouse-id}")
    private String pancakeApiWarehouseId;

    @Value("${pancake.pos.api.shop-id}")
    private String pancakeApiShopId;

    @Value("${pancake.pos.api.url}")
    private String pancakeApiUrl;

    private final PancakePropertyRepository pancakePropertyRepository;
    private final ProductRepository productRepository;
    private final VariationRepository variationRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;

    public PancakePosService(RestTemplateBuilder restTemplateBuilder,
                             PancakePropertyRepository pancakePropertyRepository,
                             ProductRepository productRepository,
                             VariationRepository variationRepository,
                             WarehouseRepository warehouseRepository,
                             OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             @Qualifier("pancakeRestTemplate") RestTemplate restTemplate) {
        this.pancakePropertyRepository = pancakePropertyRepository;
        this.productRepository = productRepository;
        this.variationRepository = variationRepository;
        this.warehouseRepository = warehouseRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restTemplate = restTemplate;
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

    @Async
    public void syncOrderToPancakeAsync(Order order) {
        log.info("Async: Starting sync order {} to Pancake POS", order.getCode());
        createOrder(order);
    }

    public boolean createOrder(Order inputOrder) {
        log.info("Pancake createOrder 1 {}", inputOrder.getCode());

        Order order = orderRepository.findOrderWithFullItems(inputOrder.getId());
        if (order == null) {
            log.error("Order not found, id={}", inputOrder.getId());
            return false;
        }

        log.info("Pancake createOrder 1.1 {}", order.getCode());
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        log.info("Pancake orderItems 2 {}", orderItems.size());

        PancakeProperties pancake = getDefault();
        Warehouse warehouse = getWarehouseDefault();

        log.info("Pancake createOrder 3 {}", order.getCode());

        if (pancake == null || warehouse == null) {
            updateOrderSyncFailure(order, "Missing Pancake config");
            return false;
        }

        log.info("Pancake createOrder 4 {}", order.getCode());

        String url = pancakeApiUrl
                + "/shops/" + pancake.getShopId()
                + "/orders?api_key=" + pancake.getToken();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 🔥 CỰC KỲ QUAN TRỌNG
            headers.set("Connection", "close");

            log.info("Pancake orderItems 4.1 {}", orderItems.size());
            MainOrderRequest body =
                    new MainOrderRequest(order, pancake, warehouse, orderItems);

            log.info("Pancake createOrder 5 {}", order.getCode());

            HttpEntity<MainOrderRequest> request =
                    new HttpEntity<>(body, headers);

            long start = System.currentTimeMillis();

            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);

            long cost = System.currentTimeMillis() - start;

            log.info("Pancake POS sync order {} took {} ms",
                    order.getCode(), cost);

            if (response.getStatusCode() != HttpStatus.CREATED) {
                updateOrderSyncFailure(order,
                        "HTTP " + response.getStatusCode());
                return false;
            }

            log.info("Pancake createOrder 6.1 {}", inputOrder.getCode());
            if (response.getBody() == null || response.getBody().isBlank()) {
                updateOrderSyncFailure(order, "Empty response body");
                return false;
            }

            log.info("Pancake createOrder 6.2 {}", inputOrder.getCode());
            updateOrderSyncSuccess(order);
            return true;

        } catch (ResourceAccessException e) {
            log.info("Pancake createOrder 6.3 {}", inputOrder.getCode());
            Throwable root = getRootCause(e);
            String msg = root != null ? root.getMessage() : e.getMessage();

            // 🎯 LỖI BẠN ĐANG GẶP
            log.error("Pancake POS timeout (443) order {}: {}",
                    order.getCode(), msg);

            updateOrderSyncFailure(order, "Timeout: " + msg);
            return false;

        } catch (Exception e) {
            log.error("Unexpected error syncing order {}",
                    order.getCode(), e);
            updateOrderSyncFailure(order, e.getMessage());
            return false;
        }
    }
    
    @Transactional
    private void updateOrderSyncSuccess(Order order) {
        // Reload order để tránh stale data
        Order freshOrder = orderRepository.findById(order.getId()).orElse(null);
        if (freshOrder == null) {
            log.error("Order {} not found when updating sync success", order.getId());
            return;
        }
        freshOrder.setStatus(OrderStatus.NEW.getValue());
        freshOrder.setCountError(0);
        freshOrder.setMessageError(null);
        orderRepository.save(freshOrder);
    }
    
    @Transactional
    private void updateOrderSyncFailure(Order order, String errorMessage) {
        // Reload order để tránh stale data và race condition
        Order freshOrder = orderRepository.findById(order.getId()).orElse(null);
        if (freshOrder == null) {
            log.error("Order {} not found when updating sync failure", order.getId());
            return;
        }
        freshOrder.setStatus(OrderStatus.SYNC_FAIL.getValue());
        freshOrder.setCountError(freshOrder.getCountError() != null ? freshOrder.getCountError() + 1 : 1);
        freshOrder.setMessageError(errorMessage);
        orderRepository.save(freshOrder);
    }
    
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null || cause == throwable) {
            return throwable;
        }
        return getRootCause(cause);
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
            // Validate input
            if (phone == null || phone.trim().isEmpty()) {
                log.error("Phone number is null or empty, cannot fetch orders from Pancake POS");
                return new ArrayList<>();
            }

            PancakeProperties pancakeProperty = getDefault();
            if (pancakeProperty == null) {
                log.error("PancakeProperties not found");
                return new ArrayList<>();
            }

            if (pancakeProperty.getShopId() == null || pancakeProperty.getToken() == null) {
                log.error("PancakeProperties missing shopId or token");
                return new ArrayList<>();
            }

            String url = pancakeApiUrl + "/shops/" + pancakeProperty.getShopId() + "/orders?api_key=" + pancakeProperty.getToken() + "&page_size=" + pageSize + "&page_number=" + pageNumber + "&search=" + phone;
            log.info("Calling Pancake POS API: {}", url.replace(pancakeProperty.getToken(), "***"));
            
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                if (response.getBody() == null || response.getBody().isEmpty()) {
                    log.info("Pancake POS API returned empty response body");
                    return new ArrayList<>();
                }
                
                JsonNode rootNode = objectMapper.readTree(response.getBody());
                String variationJson = rootNode.path("data").toString();
                
                if (variationJson == null || variationJson.isEmpty() || "null".equals(variationJson)) {
                    log.info("No order data found for phone: {}", phone);
                    return new ArrayList<>();
                }
                
                List<OrderPancakeResponse> orders = objectMapper.readValue(variationJson, objectMapper.getTypeFactory().constructCollectionType(List.class, OrderPancakeResponse.class));
                return orders != null ? orders : new ArrayList<>();
            } else {
                log.error("Failed to sync Order with Pancake POS. Status: {}, Response: {}", response.getStatusCode(), response.getBody());
                return new ArrayList<>();
            }
        } catch (HttpServerErrorException e) {
            log.error("Pancake POS API server error (5xx) for phone {}: Status={}, Response={}", phone, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return new ArrayList<>();
        } catch (HttpClientErrorException e) {
            log.error("Pancake POS API client error (4xx) for phone {}: Status={}, Response={}", phone, e.getStatusCode(), e.getResponseBodyAsString(), e);
            return new ArrayList<>();
        } catch (RestClientException e) {
            log.error("Rest client error while calling Pancake POS API for phone {}: {}", phone, e.getMessage(), e);
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("Unexpected error occurred while syncing Order from Pancake POS for phone {}: {}", phone, e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    private PancakeProperties getDefault() {
        return pancakePropertyRepository.findFirstByDeletedIsFalse();
    }

    private Warehouse getWarehouseDefault() {
        return warehouseRepository.findFirstByDeletedIsFalse();
    }
}
