package vn.co.cake.service.external;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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
import vn.co.cake.payment.support.PaymentCheckoutFlowLog;
import vn.co.cake.repository.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.http.NoHttpResponseException;
import org.springframework.beans.factory.annotation.Qualifier;
import vn.co.cake.helper.EmailService;

@Service
@Slf4j
public class PancakePosService {

    @Value("${pancake.pos.api.url}")
    private String pancakeApiUrl;

    /** Số dòng tối đa gửi qua email (body quá dài dễ lỗi transport Brevo). */
    private static final int PANCAKE_ERROR_EMAIL_MAX_LINES = 11;

    /** Nghỉ giữa từng phần email để tránh gửi liên tục bị coi là spam / rate limit. */
    @Value("${pancake.sync-fail.email.part-delay-ms:800}")
    private long pancakeErrorEmailPartDelayMs;

    private final PancakePropertyRepository pancakePropertyRepository;
    private final ProductRepository productRepository;
    private final VariationRepository variationRepository;
    private final WarehouseRepository warehouseRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestTemplate restTemplate;
    private final EmailService emailService;

    public PancakePosService(RestTemplateBuilder restTemplateBuilder,
                             PancakePropertyRepository pancakePropertyRepository,
                             ProductRepository productRepository,
                             VariationRepository variationRepository,
                             WarehouseRepository warehouseRepository,
                             OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             @Qualifier("pancakeRestTemplate") RestTemplate restTemplate,
                             EmailService emailService) {
        this.pancakePropertyRepository = pancakePropertyRepository;
        this.productRepository = productRepository;
        this.variationRepository = variationRepository;
        this.warehouseRepository = warehouseRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restTemplate = restTemplate;
        this.emailService = emailService;
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
            if (!hasApiCredentials(pancakeProperty)) {
                log.error("PancakeProperties not found in DB (pancake_properties)");
                return null;
            }
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
        syncOrderToPancakeAsync(order, order != null ? order.getCode() : null);
    }

    @Async
    public void syncOrderToPancakeAsync(Order order, String traceId) {
        if (order == null) {
            PaymentCheckoutFlowLog.step(traceId, 11, "LỖI: sync Pancake — order null");
            return;
        }
        PaymentCheckoutFlowLog.step(traceId, 11,
                "Async sync Pancake bắt đầu — orderId=%s, orderCode=%s", order.getId(), order.getCode());
        boolean success = createOrder(order, traceId);
        if (success) {
            PaymentCheckoutFlowLog.step(traceId, 15,
                    "Hoàn tất: đơn đã tạo trên Pancake POS — orderCode=%s", order.getCode());
        } else {
            PaymentCheckoutFlowLog.step(traceId, 14,
                    "Kết thúc sync Pancake THẤT BẠI — orderCode=%s (Job002 có thể retry)", order.getCode());
        }
    }

    public boolean createOrder(Order inputOrder) {
        return createOrder(inputOrder, inputOrder != null ? inputOrder.getCode() : null);
    }

    public boolean createOrder(Order inputOrder, String traceId) {
        if (inputOrder == null) {
            PaymentCheckoutFlowLog.step(traceId, 12, "LỖI: createOrder — inputOrder null");
            return false;
        }

        Order order = orderRepository.findOrderWithFullItems(inputOrder.getId());
        if (order == null) {
            PaymentCheckoutFlowLog.step(traceId, 12,
                    "LỖI: không load được đơn + items từ DB — orderId=%s", inputOrder.getId());
            log.error("Order not found, id={}", inputOrder.getId());
            return false;
        }

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderId(order.getId());
        PaymentCheckoutFlowLog.step(traceId, 12,
                "Đã load đơn từ DB — orderCode=%s, orderItems=%s", order.getCode(), orderItems.size());

        PancakeProperties pancake = getDefault();
        Warehouse warehouse = getWarehouseDefault();

        if (!hasOrderSyncConfig(pancake) || warehouse == null) {
            PaymentCheckoutFlowLog.step(traceId, 12,
                    "LỖI: thiếu cấu hình Pancake — kiểm tra bảng pancake_properties (shop_id, token, warehouse_id) và warehouse");
            log.error("Missing Pancake config from DB for order {} — pancake_properties={}, warehouse={}",
                    order.getCode(), pancake != null, warehouse != null);
            updateOrderSyncFailure(order, "Missing Pancake config");
            return false;
        }

        PaymentCheckoutFlowLog.step(traceId, 13,
                "Gọi Pancake POST create order — shopId=%s, warehouseId=%s, warehouseName=%s",
                pancake.getShopId(), pancake.getWarehouseId(), warehouse.getName());

        String url = pancakeApiUrl
                + "/shops/" + pancake.getShopId()
                + "/orders?api_key=" + pancake.getToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Connection", "close");
        headers.set("User-Agent", "DebaseApp/1.0 (Java)");

        MainOrderRequest body =
                new MainOrderRequest(order, pancake, warehouse, orderItems);
        HttpEntity<MainOrderRequest> request = new HttpEntity<>(body, headers);

        try {
            long start = System.currentTimeMillis();
            ResponseEntity<String> response =
                    restTemplate.postForEntity(url, request, String.class);
            long cost = System.currentTimeMillis() - start;

            PaymentCheckoutFlowLog.step(traceId, 13,
                    "Pancake API phản hồi — http=%s, durationMs=%s", response.getStatusCode(), cost);

            if (response.getStatusCode() != HttpStatus.CREATED) {
                PaymentCheckoutFlowLog.step(traceId, 14,
                        "LỖI Pancake HTTP %s — orderCode=%s", response.getStatusCode(), order.getCode());
                logCurlForPancakeOrder(url, headers, body, order.getCode());
                updateOrderSyncFailure(order, "HTTP " + response.getStatusCode());
                return false;
            }
            if (response.getBody() == null || response.getBody().isBlank()) {
                PaymentCheckoutFlowLog.step(traceId, 14, "LỖI Pancake: response body rỗng — orderCode=%s", order.getCode());
                logCurlForPancakeOrder(url, headers, body, order.getCode());
                updateOrderSyncFailure(order, "Empty response body");
                return false;
            }
            updateOrderSyncSuccess(order, response.getBody());
            PaymentCheckoutFlowLog.step(traceId, 14,
                    "Pancake tạo đơn thành công (HTTP 201) — orderCode=%s", order.getCode());
            return true;

        } catch (ResourceAccessException e) {
            logCurlForPancakeOrder(url, headers, body, order.getCode());
            Throwable root = getRootCause(e);
            String msg = root != null ? root.getMessage() : e.getMessage();
            String rootClass = root != null ? root.getClass().getSimpleName() : e.getClass().getSimpleName();
            boolean isNoResponse = root instanceof NoHttpResponseException;
            String failureMsg = isNoResponse
                    ? "Server closed connection. Job002 will retry."
                    : "Timeout: " + msg;
            PaymentCheckoutFlowLog.step(traceId, 14,
                    "LỖI kết nối Pancake — orderCode=%s, %s [%s]", order.getCode(), msg, rootClass);
            log.warn("Pancake POS order {}: {} [{}]", order.getCode(), msg, rootClass);
            updateOrderSyncFailure(order, truncateMessage(failureMsg, 255));
            return false;
        } catch (Exception e) {
            logCurlForPancakeOrder(url, headers, body, order.getCode());
            log.error("Unexpected error syncing order {}", order.getCode(), e);
            String errMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            PaymentCheckoutFlowLog.step(traceId, 14,
                    "LỖI không mong đợi khi sync Pancake — orderCode=%s, %s", order.getCode(), errMsg);
            updateOrderSyncFailure(order, truncateMessage(errMsg, 255));
            return false;
        }
    }
    
    @Transactional
    public void enrichOrdersFromPancake(List<Order> orders) {
        if (CollectionUtils.isEmpty(orders)) {
            return;
        }
        for (Order order : orders) {
            if (order == null || StringUtils.isBlank(order.getPhone())) {
                continue;
            }
            if (StringUtils.isNotBlank(order.getPancakeStatusName())
                    && StringUtils.isNotBlank(order.getShippingPartner())) {
                continue;
            }
            enrichOrderFromPancakeList(order);
        }
    }

    /**
     * Job định kỳ: gom đơn theo SĐT, gọi Pancake tối thiểu rồi lưu status / tracking vào DB.
     */
    @Transactional
    public void syncOrdersFromPancakeHourly(int batchSize) {
        List<Order> orders = orderRepository.findOrdersForPancakeHourlySync(batchSize);
        if (CollectionUtils.isEmpty(orders)) {
            log.info("Pancake hourly sync: no orders due for refresh");
            return;
        }

        Map<String, List<Order>> byPhone = new LinkedHashMap<>();
        for (Order order : orders) {
            if (order == null || StringUtils.isBlank(order.getPhone())) {
                continue;
            }
            byPhone.computeIfAbsent(order.getPhone().trim(), k -> new ArrayList<>()).add(order);
        }

        int synced = 0;
        for (Map.Entry<String, List<Order>> entry : byPhone.entrySet()) {
            String phone = entry.getKey();
            List<Order> phoneOrders = entry.getValue();
            List<Order> needListLookup = new ArrayList<>();
            for (Order order : phoneOrders) {
                try {
                    if (StringUtils.isNotBlank(order.getPancakeOrderId())) {
                        syncOrderFromPancakeDetail(order);
                        synced++;
                    } else {
                        needListLookup.add(order);
                    }
                } catch (Exception ex) {
                    log.warn("Pancake hourly sync failed for order {}: {}", order.getCode(), ex.getMessage());
                }
            }
            if (needListLookup.isEmpty()) {
                continue;
            }
            try {
                List<OrderPancakeResponse> pancakeList = getAllOrderPancake(phone, 0, 200);
                for (Order order : needListLookup) {
                    try {
                        syncOrderFromPancakeList(order, pancakeList);
                        synced++;
                    } catch (Exception ex) {
                        log.warn("Pancake hourly sync failed for order {}: {}", order.getCode(), ex.getMessage());
                    }
                }
            } catch (Exception ex) {
                log.warn("Pancake hourly sync failed for phone {}: {}", phone, ex.getMessage());
            }
        }
        log.info("Pancake hourly sync: refreshed {} orders across {} phone(s)", synced, byPhone.size());
    }

    private void syncOrderFromPancakeDetail(Order order) {
        OrderPancakeResponse detail = getOrderPancakeDetail(order.getPancakeOrderId());
        if (detail != null) {
            applyPancakeOrderResponse(order, detail);
        }
        order.setPancakeSyncedAt(new Date());
        orderRepository.save(order);
    }

    private void syncOrderFromPancakeList(Order order, List<OrderPancakeResponse> pancakeList) {
        if (pancakeList != null && !pancakeList.isEmpty()) {
            OrderPancakeResponse match = findMatchingPancakeOrder(pancakeList, order.getCode());
            if (match != null) {
                enrichWithPancakeOrderDetail(match);
                applyPancakeOrderResponse(order, match);
            }
        }
        order.setPancakeSyncedAt(new Date());
        orderRepository.save(order);
    }

    @Transactional
    private void updateOrderSyncSuccess(Order order, String createResponseBody) {
        Order freshOrder = orderRepository.findById(order.getId()).orElse(null);
        if (freshOrder == null) {
            log.error("Order {} not found when updating sync success", order.getId());
            return;
        }
        applyPancakeMetadata(freshOrder, createResponseBody);
        if (StringUtils.isBlank(freshOrder.getPancakeStatusName())) {
            enrichOrderFromPancakeList(freshOrder);
        }
        freshOrder.setStatus(OrderStatus.NEW.getValue());
        freshOrder.setCountError(0);
        freshOrder.setMessageError(null);
        orderRepository.save(freshOrder);
    }

    private void enrichOrderFromPancakeList(Order order) {
        List<OrderPancakeResponse> pancakeOrders = getAllOrderPancake(order.getPhone(), 0, 200);
        if (pancakeOrders == null || pancakeOrders.isEmpty()) {
            return;
        }
        OrderPancakeResponse match = findMatchingPancakeOrder(pancakeOrders, order.getCode());
        if (match == null) {
            return;
        }
        enrichWithPancakeOrderDetail(match);
        applyPancakeOrderResponse(order, match);
        order.setPancakeSyncedAt(new Date());
        orderRepository.save(order);
        log.info("Enriched order {} from Pancake list: status={}, partner={}",
                order.getCode(), order.getPancakeStatusName(), order.getShippingPartner());
    }

    private OrderPancakeResponse findMatchingPancakeOrder(List<OrderPancakeResponse> pancakeOrders, String orderCode) {
        if (StringUtils.isBlank(orderCode)) {
            return null;
        }
        for (OrderPancakeResponse pancakeOrder : pancakeOrders) {
            if (orderCode.equals(pancakeOrder.getCustomId()) || orderCode.equals(pancakeOrder.getId())) {
                return pancakeOrder;
            }
        }
        return null;
    }

    private void applyPancakeMetadata(Order order, String responseBody) {
        if (StringUtils.isBlank(responseBody)) {
            return;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode root = mapper.readTree(responseBody);
            JsonNode data = root.has("data") ? root.get("data") : root;
            if (data.isArray() && data.size() > 0) {
                data = data.get(0);
            }
            applyPancakeJsonNode(order, data);
        } catch (Exception ex) {
            log.warn("Cannot parse Pancake create-order response for {}: {}", order.getCode(), ex.getMessage());
        }
    }

    private void applyPancakeOrderResponse(Order order, OrderPancakeResponse pancakeOrder) {
        if (StringUtils.isNotBlank(pancakeOrder.getId())) {
            order.setPancakeOrderId(pancakeOrder.getId());
        }
        if (StringUtils.isNotBlank(pancakeOrder.getStatus_name())) {
            order.setPancakeStatusName(pancakeOrder.getStatus_name());
        }
        String partner = pancakeOrder.resolveShippingPartner();
        if (StringUtils.isNotBlank(partner)) {
            order.setShippingPartner(partner);
        }
        if (StringUtils.isNotBlank(pancakeOrder.getTracking_link())) {
            order.setTrackingLink(pancakeOrder.getTracking_link());
        }
    }

    private void applyPancakeJsonNode(Order order, JsonNode data) {
        if (data == null || data.isNull()) {
            return;
        }
        String pancakeId = textValue(data, "id");
        if (StringUtils.isNotBlank(pancakeId)) {
            order.setPancakeOrderId(pancakeId);
        }
        String statusName = textValue(data, "status_name");
        if (StringUtils.isNotBlank(statusName)) {
            order.setPancakeStatusName(statusName);
        }
        String trackingLink = textValue(data, "tracking_link");
        if (StringUtils.isNotBlank(trackingLink)) {
            order.setTrackingLink(trackingLink);
        }
        String partner = resolvePartnerName(textValue(data, "partner_name"), textValue(data, "partner"));
        if (partner == null && data.has("partner") && data.get("partner").isObject()) {
            partner = textValue(data.get("partner"), "name");
        }
        if (StringUtils.isNotBlank(partner)) {
            order.setShippingPartner(partner);
        }
    }

    private static String resolvePartnerName(String partnerName, String partner) {
        if (StringUtils.isNotBlank(partnerName)) {
            return partnerName.trim();
        }
        if (StringUtils.isNotBlank(partner)) {
            return partner.trim();
        }
        return null;
    }

    private static String textValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        return node.get(field).asText();
    }
    
    @Transactional
    private void updateOrderSyncFailure(Order order, String errorMessage) {
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

    /** Giới hạn độ dài message trước khi lưu DB (cột thường VARCHAR 255). */
    private static String truncateMessage(String msg, int maxLen) {
        if (msg == null || maxLen <= 3) return msg;
        return msg.length() <= maxLen ? msg : msg.substring(0, maxLen - 3) + "...";
    }

    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable.getCause();
        if (cause == null || cause == throwable) {
            return throwable;
        }
        return getRootCause(cause);
    }

    /**
     * Ghi file .txt chứa URL, curl và body JSON để copy test Postman/terminal.
     * File lưu tại: log/pancake-curl-{orderCode}-{timestamp}.txt
     */
    private void logCurlForPancakeOrder(String url, HttpHeaders headers, MainOrderRequest body, String orderCode) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            String bodyJson = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(body);
            // Escape single quote cho bash: ' -> '\''
            String bodyEscaped = bodyJson.replace("'", "'\\''");
            String curl = "curl -X POST '" + url + "'"
                    + " -H 'Content-Type: application/json'"
                    + " -H 'Connection: close'"
                    + " -d '" + bodyEscaped + "'";

            String safeCode = (orderCode != null ? orderCode : "unknown").replaceAll("[^a-zA-Z0-9_-]", "_");
            String fileName = "pancake-curl-" + safeCode + "-" + System.currentTimeMillis() + ".txt";
            Path logDir = Paths.get("log");
            Files.createDirectories(logDir);
            Path file = logDir.resolve(fileName);

            String content = "-- Curl (copy to terminal) ---\n" + curl + "\n";
            Files.writeString(file, content, StandardCharsets.UTF_8);

            log.info("Pancake POS request saved to file: {}", file.toAbsolutePath());
            sendPancakeErrorEmail(orderCode, content);
        } catch (Exception e) {
            log.warn("Could not save Pancake curl to file: {}", e.getMessage());
        }
    }

    /**
     * Gửi nội dung lỗi thành nhiều email (mỗi email tối đa {@link #PANCAKE_ERROR_EMAIL_MAX_LINES} dòng).
     * Subject: {@code DEBASE SYNC_FAIL <orderCode> p.<page>/<total>} để ghép lại đúng thứ tự thành 1 file.
     */
    private void sendPancakeErrorEmail(String orderCode, String emailContent) {
        String emailTo = "haitv@ominext.com";
        if (emailContent == null || emailContent.isBlank()) {
            return;
        }
        String normalized = emailContent.replace("\r\n", "\n");
        String[] lines = normalized.split("\n", -1);
        int lineCount = lines.length;
        int chunk = PANCAKE_ERROR_EMAIL_MAX_LINES;
        int totalParts = (lineCount + chunk - 1) / chunk;
        if (totalParts <= 0) {
            return;
        }
        String safeOrder = orderCode != null ? orderCode : "unknown";
        int sentOk = 0;
        for (int p = 1; p <= totalParts; p++) {
            int from = (p - 1) * chunk;
            int to = Math.min(from + chunk, lineCount);
            String chunkBody = String.join("\n", Arrays.copyOfRange(lines, from, to));
            String subject = String.format("DEBASE SYNC_FAIL %s p.%d/%d", safeOrder, p, totalParts);
            boolean sent = emailService.sendPlainTextEmail(emailTo, subject, chunkBody);
            if (sent) {
                sentOk++;
                log.info("Sent Pancake POS error email part {}/{} for order {} to {}", p, totalParts, orderCode, emailTo);
            } else {
                log.warn("Could not send Pancake POS error email part {}/{} for order {} to {}", p, totalParts, orderCode, emailTo);
            }
            if (p < totalParts && pancakeErrorEmailPartDelayMs > 0) {
                try {
                    Thread.sleep(pancakeErrorEmailPartDelayMs);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("Interrupted during delay between Pancake error email parts (after {}/{})", p, totalParts);
                    break;
                }
            }
        }
        if (sentOk == totalParts) {
            log.info("Sent all {} Pancake POS error email parts for order {} to {}", totalParts, orderCode, emailTo);
        }
    }
    
    public List<VariationResponse> getAllProductPancake(int pageNumber, int pageSize) {
        try {
            PancakeProperties pancakeProperty = getDefault();
            if (!hasApiCredentials(pancakeProperty)) {
                log.error("PancakeProperties not found in DB (pancake_properties)");
                return null;
            }
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

    /**
     * Lấy chi tiết một đơn trên Pancake POS (GET /shops/{shopId}/orders/{orderId}).
     * Response chứa {@code tracking_link} dùng cho nút "Xem chi tiết".
     */
    public OrderPancakeResponse getOrderPancakeDetail(String pancakeOrderId) {
        if (StringUtils.isBlank(pancakeOrderId)) {
            return null;
        }
        try {
            PancakeProperties pancakeProperty = getDefault();
            if (!hasApiCredentials(pancakeProperty)) {
                log.error("PancakeProperties not found in DB (pancake_properties)");
                return null;
            }
            String url = pancakeApiUrl + "/shops/" + pancakeProperty.getShopId()
                    + "/orders/" + pancakeOrderId + "?api_key=" + pancakeProperty.getToken();
            log.info("Calling Pancake POS order detail: shop={}, orderId={}",
                    pancakeProperty.getShopId(), pancakeOrderId);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() != HttpStatus.OK || StringUtils.isBlank(response.getBody())) {
                log.warn("Pancake order detail failed for {}: status={}", pancakeOrderId, response.getStatusCode());
                return null;
            }

            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode dataNode = rootNode.path("data");
            if (dataNode.isMissingNode() || dataNode.isNull()) {
                dataNode = rootNode;
            }
            if (dataNode.isArray() && dataNode.size() > 0) {
                dataNode = dataNode.get(0);
            }
            return OrderPancakeResponse.fromPancakeJsonNode(dataNode, objectMapper);
        } catch (Exception e) {
            log.error("Failed to fetch Pancake order detail for id {}: {}", pancakeOrderId, e.getMessage());
            return null;
        }
    }

    /**
     * Bổ sung {@code tracking_link} và field hiển thị từ API order detail (không dựng URL search).
     */
    public void enrichWithPancakeOrderDetail(OrderPancakeResponse order) {
        if (order == null || StringUtils.isBlank(order.getId())) {
            return;
        }
        OrderPancakeResponse detail = getOrderPancakeDetail(order.getId());
        if (detail == null) {
            return;
        }
        order.mergeFromDetail(detail);
        log.debug("Pancake order {} detail enriched, tracking_link={}", order.getCustomId(), order.getTracking_link());
    }

    public List<OrderPancakeResponse> getAllOrderPancake(String phone, int pageNumber, int pageSize) {
        try {
            // Validate input
            if (phone == null || phone.trim().isEmpty()) {
                log.error("Phone number is null or empty, cannot fetch orders from Pancake POS");
                return new ArrayList<>();
            }

            PancakeProperties pancakeProperty = getDefault();
            if (!hasApiCredentials(pancakeProperty)) {
                log.error("PancakeProperties not found in DB (pancake_properties)");
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
                JsonNode dataNode = rootNode.path("data");

                if (dataNode.isMissingNode() || dataNode.isNull() || !dataNode.isArray() || dataNode.isEmpty()) {
                    log.info("No order data found for phone: {}", phone);
                    return new ArrayList<>();
                }

                List<OrderPancakeResponse> orders = OrderPancakeResponse.parseListFromData(dataNode, objectMapper);
                return orders;
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

    public String getShopId() {
        PancakeProperties pancake = getDefault();
        return pancake != null ? pancake.getShopId() : null;
    }

    /** Shop, token, warehouse_id Pancake — chỉ từ bảng {@code pancake_properties}. */
    private PancakeProperties getDefault() {
        return pancakePropertyRepository.findFirstByDeletedIsFalse();
    }

    private Warehouse getWarehouseDefault() {
        return warehouseRepository.findFirstByDeletedIsFalse();
    }

    private static boolean hasApiCredentials(PancakeProperties properties) {
        return properties != null
                && StringUtils.isNotBlank(properties.getShopId())
                && StringUtils.isNotBlank(properties.getToken());
    }

    private static boolean hasOrderSyncConfig(PancakeProperties properties) {
        return hasApiCredentials(properties) && StringUtils.isNotBlank(properties.getWarehouseId());
    }

}
