package vn.co.cake.service.external;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.scheduling.annotation.Async;
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
        // Configure RestTemplate with timeouts to prevent connection hangs
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(10000) // 10 seconds connection timeout
                .setSocketTimeout(30000) // 30 seconds socket timeout
                .setConnectionRequestTimeout(10000) // 10 seconds connection request timeout
                .build();
        
        CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setMaxConnTotal(100)
                .setMaxConnPerRoute(20)
                .build();
        
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setHttpClient(httpClient);
        factory.setConnectTimeout(10000);
        factory.setReadTimeout(30000);
        factory.setConnectionRequestTimeout(10000);
        
        this.restTemplate = new RestTemplate(factory);
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

    // @Async
    public void syncOrderToPancakeAsync(Order order) {
        log.info("Async: Starting sync order {} to Pancake POS", order.getCode());
        createOrder(order);
    }

    public boolean createOrder(Order order) {
        if (order == null) {
            log.error("Order is null, cannot sync to Pancake POS");
            return false;
        }
        
        // Validate configuration
        PancakeProperties pancakeProperty = this.getDefault();
        if (pancakeProperty == null) {
            log.error("PancakeProperties not found, cannot sync order {}", order.getCode());
            updateOrderSyncFailure(order, "PancakeProperties not found");
            return false;
        }
        
        Warehouse warehouse = this.getWarehouseDefault();
        if (warehouse == null) {
            log.error("Warehouse not found, cannot sync order {}", order.getCode());
            updateOrderSyncFailure(order, "Warehouse not found");
            return false;
        }
        
        try {
            // Build request
            String url = pancakeApiUrl + "/shops/" + pancakeProperty.getShopId() + "/orders?api_key=" + pancakeProperty.getToken();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            MainOrderRequest mainOrderRequest = new MainOrderRequest(order, pancakeProperty, warehouse);
            HttpEntity<MainOrderRequest> request = new HttpEntity<>(mainOrderRequest, headers);

            log.info("Syncing order {} to Pancake POS: {}", order.getCode(), url.replace(pancakeProperty.getToken(), "***"));
            
            // Call API - CHỈ 1 LẦN, KHÔNG RETRY (Job002 sẽ retry nếu fail)
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            
            if (response.getStatusCode() == HttpStatus.CREATED) {
                String responseBody = response.getBody();
                if (responseBody == null || responseBody.trim().isEmpty()) {
                    log.error("Pancake POS returned empty response body for order {}", order.getCode());
                    updateOrderSyncFailure(order, "Empty response body");
                    return false;
                }
                
                try {
                    JsonNode rootNode = objectMapper.readTree(responseBody);
                    JsonNode dataNode = rootNode.path("data");
                    if (dataNode.isMissingNode() || dataNode.isNull()) {
                        log.error("Pancake POS response for order {} missing data field. Response: {}", order.getCode(), responseBody);
                        updateOrderSyncFailure(order, "Missing data field in response");
                        return false;
                    }
                    
                    // SUCCESS: Update order status
                    updateOrderSyncSuccess(order);
                    log.info("Successfully synced order {} to Pancake POS. Response data: {}", order.getCode(), dataNode.toString());
                    return true;
                } catch (Exception parseException) {
                    log.error("Failed to parse Pancake POS response for order {}. Response body: {}. Parse error: {}", 
                            order.getCode(), responseBody, parseException.getMessage(), parseException);
                    updateOrderSyncFailure(order, "Parse error: " + parseException.getMessage());
                    return false;
                }
            } else {
                log.error("Failed to sync order {} with Pancake POS. Status: {}, Response: {}", 
                        order.getCode(), response.getStatusCode(), response.getBody());
                updateOrderSyncFailure(order, "HTTP Status: " + response.getStatusCode());
                return false;
            }
        } catch (HttpClientErrorException e) {
            // 4xx errors - client errors
            log.error("Client error (4xx) while syncing order {} to Pancake POS: Status={}, Response={}", 
                    order.getCode(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            updateOrderSyncFailure(order, "Client error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return false;
        } catch (HttpServerErrorException e) {
            // 5xx errors - server errors
            log.error("Server error (5xx) while syncing order {} to Pancake POS: Status={}, Response={}", 
                    order.getCode(), e.getStatusCode(), e.getResponseBodyAsString(), e);
            updateOrderSyncFailure(order, "Server error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            return false;
        } catch (ResourceAccessException e) {
            // Connection/timeout errors
            Throwable rootCause = getRootCause(e);
            log.error("Connection error while syncing order {} to Pancake POS: {}", 
                    order.getCode(), rootCause != null ? rootCause.getMessage() : e.getMessage(), e);
            updateOrderSyncFailure(order, "Connection error: " + (rootCause != null ? rootCause.getMessage() : e.getMessage()));
            return false;
        } catch (Exception e) {
            // Other unexpected errors
            log.error("Unexpected error occurred while syncing order {} to Pancake POS: {}", 
                    order.getCode(), e.getMessage(), e);
            updateOrderSyncFailure(order, "Unexpected error: " + e.getMessage());
            return false;
        }
    }
    
    private void updateOrderSyncSuccess(Order order) {
        order.setStatus(OrderStatus.NEW.getValue());
        order.setCountError(0);
        order.setMessageError(null);
        orderRepository.save(order);
    }
    
    private void updateOrderSyncFailure(Order order, String errorMessage) {
        order.setStatus(OrderStatus.SYNC_FAIL.getValue());
        order.setCountError(order.getCountError() != null ? order.getCountError() + 1 : 1);
        order.setMessageError(errorMessage);
        orderRepository.save(order);
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
