# Tổng hợp thay đổi code - Review trước khi thực hiện

## Tổng quan
- **Mục tiêu**: Refactor code Order, loại bỏ retry logic, tạo Job002 để retry sync Pancake POS
- **Số files mới**: 3 files
- **Số files sửa**: 5 files

---

## 📁 FILES MỚI CẦN TẠO

### 1. `src/main/java/vn/co/cake/enums/OrderStatus.java`
```java
package vn.co.cake.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    NEW("NEW"),
    SYNC_FAIL("SYNC_FAIL"),
    PENDING_SYNC("PENDING_SYNC");
    
    private final String value;
    
    OrderStatus(String value) {
        this.value = value;
    }
}
```

### 2. `src/main/java/vn/co/cake/constants/OrderConstants.java`
```java
package vn.co.cake.constants;

public class OrderConstants {
    public static final String VOUCHER_SHIPPING_FEE = "SHIPPING_FEE";
    public static final long FREE_SHIPPING_THRESHOLD = 1_000_000L;
    public static final int MAX_PANCAKE_SYNC_RETRIES = 5;
}
```

### 3. `src/main/java/vn/co/cake/job/Job002.java`
```java
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
    
    @Scheduled(cron = "0 */10 * * * ?") // Chạy mỗi 10 phút
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
                    log.warn("Job002: Failed to sync order {} to Pancake POS (will retry next time)", order.getCode());
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
```

---

## 📝 FILES CẦN SỬA

### 1. `src/main/java/vn/co/cake/entity/Order.java`

**Thêm 2 fields mới:**
```java
// Thêm sau line 36 (sau voucher field)
private Integer countError = 0;
private String messageError;
```

**File sau khi sửa:**
```java
package vn.co.cake.entity;

import javax.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Entity
@Table(name = "orders")
public class Order extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String code;

    @ManyToOne
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    private BigDecimal totalAmount;
    private BigDecimal shippingFee;
    private BigDecimal prepaid;
    private String status;
    private String shippingAddress;
    private String paymentMethod;
    
    private String fullName;
    private String email;
    private String phone;
    private String note;
    private String voucher;
    
    // THÊM MỚI
    private Integer countError = 0;
    private String messageError;

    @Column(columnDefinition = "boolean default false")
    private boolean deleted;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> orderItems;
}
```

---

### 2. `src/main/java/vn/co/cake/repository/OrderRepository.java`

**Thêm method mới để query orders SYNC_FAIL:**
```java
// Thêm vào cuối file, trước dấu }
List<Order> findByStatusAndCountErrorLessThan(String status, int maxCountError);
```

**File sau khi sửa:**
```java
package vn.co.cake.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vn.co.cake.entity.Order;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Order findFirstByCode(String code);
    Order findFirstByAccountIdOrderByCreatedDesc(Long accountId);
    
    // THÊM MỚI
    List<Order> findByStatusAndCountErrorLessThan(String status, int maxCountError);
}
```

---

### 3. `src/main/java/vn/co/cake/service/external/PancakePosService.java`

**THAY ĐỔI LỚN: Loại bỏ toàn bộ retry logic, đơn giản hóa method createOrder()**

**XÓA:**
- Toàn bộ retry loop (for loop với maxRetries)
- Sleep/exponential backoff logic
- Nested retry logic trong các catch blocks
- Logic set deleted = true

**THAY ĐỔI:**
- Chỉ call API 1 lần duy nhất
- Nếu thành công: update `status = "NEW"`, `countError = 0`, clear `messageError`
- Nếu thất bại: update `status = "SYNC_FAIL"`, increment `countError++`, set `messageError`

**Method createOrder() sau khi refactor:**
```java
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
        
        // Call API - CHỈ 1 LẦN, KHÔNG RETRY
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

// Helper methods mới
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
```

**Thêm import:**
```java
import vn.co.cake.enums.OrderStatus;
```

---

### 4. `src/main/java/vn/co/cake/service/impl/OrderServiceImpl.java`

**THAY ĐỔI:**
- Sử dụng OrderStatus enum thay vì magic string "NEW"
- Sử dụng OrderConstants thay vì magic values
- Thêm null check cho Product

**Thay đổi cụ thể:**

**Line 1-24: Thêm imports**
```java
import vn.co.cake.enums.OrderStatus;
import vn.co.cake.constants.OrderConstants;
```

**Line 89: Thay đổi**
```java
// CŨ:
Voucher shippingFee = voucherRepository.findFirstByCodeAndDeletedIsFalse("SHIPPING_FEE");

// MỚI:
Voucher shippingFee = voucherRepository.findFirstByCodeAndDeletedIsFalse(OrderConstants.VOUCHER_SHIPPING_FEE);
```

**Line 92: Thay đổi**
```java
// CŨ:
if (shippingFee != null && totalPriceOrder.longValue() < 2000000) {

// MỚI:
if (shippingFee != null && totalPriceOrder.longValue() < OrderConstants.FREE_SHIPPING_THRESHOLD) {
```

**Line 100: Thay đổi**
```java
// CŨ:
order.setStatus("NEW");

// MỚI:
order.setStatus(OrderStatus.NEW.getValue());
```

**Line 124: Thêm null check**
```java
// CŨ:
Product product = productRepository.findFirstByProductPancakeId(variation.getPancakeProductId());

// MỚI:
Product product = productRepository.findFirstByProductPancakeId(variation.getPancakeProductId());
if (Objects.isNull(product)) {
    throw new CommonServletException("Sản phẩm không tồn tại trong hệ thống!");
}
```

---

### 5. `src/main/java/vn/co/cake/controller/external/MomoController.java`

**THAY ĐỔI:**
- Báo thành công ngay sau khi tạo order (không đợi Pancake sync)
- Gọi async method để sync Pancake POS

**Thay đổi cụ thể:**

**Line 15: Thêm import**
```java
import org.springframework.scheduling.annotation.Async;
```

**Method createPayment() - Thay đổi từ line 54-68:**

**CŨ:**
```java
Order order = orderService.create(loginInfo.getId(), cartForm.getOrderItems(), request);
PaymentRequest paymentRequest = new PaymentRequest();
paymentRequest.setOrderId(order.getId());
paymentRequest.setAmount(order.getTotalAmount());

boolean orderToPancakeSuccess = pancakePosService.createOrder(order);
if (orderToPancakeSuccess) {
    this.removeCartItemOrder(loginInfo.getId(), cartForm);
} else {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Order Sản phẩm thất bại!");
}
return ResponseEntity.ok("Order Sản phẩm thành công!");
```

**MỚI:**
```java
Order order = orderService.create(loginInfo.getId(), cartForm.getOrderItems(), request);
PaymentRequest paymentRequest = new PaymentRequest();
paymentRequest.setOrderId(order.getId());
paymentRequest.setAmount(order.getTotalAmount());

// Báo thành công ngay, không đợi Pancake sync
this.removeCartItemOrder(loginInfo.getId(), cartForm);

// Sync Pancake POS async (Job002 sẽ retry nếu fail)
pancakePosService.syncOrderToPancakeAsync(order);

return ResponseEntity.ok("Order Sản phẩm thành công!");
```

**Thêm method mới trong PancakePosService:**
```java
@Async
public void syncOrderToPancakeAsync(Order order) {
    log.info("Async: Starting sync order {} to Pancake POS", order.getCode());
    createOrder(order);
}
```

---

## 📊 DATABASE MIGRATION

**File: `init/init.sql` hoặc tạo migration mới**

**Thêm 2 columns vào bảng orders:**
```sql
ALTER TABLE orders 
ADD COLUMN IF NOT EXISTS count_error INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS message_error VARCHAR(1000);
```

---

## 📋 TÓM TẮT THAY ĐỔI

### Files mới (3):
1. `OrderStatus.java` - Enum cho order status
2. `OrderConstants.java` - Constants cho magic values
3. `Job002.java` - Scheduled job retry sync mỗi 10 phút

### Files sửa (5):
1. `Order.java` - Thêm countError, messageError
2. `OrderRepository.java` - Thêm method query SYNC_FAIL orders
3. `PancakePosService.java` - **LOẠI BỎ retry logic, đơn giản hóa createOrder()**
4. `OrderServiceImpl.java` - Sử dụng enum/constants, thêm null check
5. `MomoController.java` - Báo thành công ngay, async sync Pancake

### Database:
- Thêm 2 columns: `count_error`, `message_error`

---

## ⚠️ LƯU Ý QUAN TRỌNG

1. **Không tạo trùng order**: Job002 chỉ retry orders có `status = "SYNC_FAIL"`. Khi thành công, status = "NEW" nên không retry lại.

2. **Giới hạn retry**: Chỉ retry nếu `countError < 5` (MAX_PANCAKE_SYNC_RETRIES).

3. **Async method**: Cần đảm bảo `@EnableAsync` đã được config (đã có trong ThreadPoolManager).

4. **Database migration**: Cần chạy SQL migration trước khi deploy code.

5. **Testing**: Cần test kỹ:
   - Tạo order mới → báo thành công ngay
   - Sync Pancake thành công → status = "NEW"
   - Sync Pancake thất bại → status = "SYNC_FAIL"
   - Job002 retry → update status đúng

---

## ✅ CHECKLIST TRƯỚC KHI THỰC HIỆN

- [ ] Review tất cả code changes
- [ ] Đảm bảo database migration được chạy
- [ ] Test flow tạo order
- [ ] Test Job002 retry logic
- [ ] Kiểm tra không có breaking changes
- [ ] Backup database trước khi deploy

