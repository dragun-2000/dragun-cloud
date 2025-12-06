# Phân tích Logic Hiển thị Màn hình Order Status

## Tổng quan
Màn hình `order-status.html` hiển thị danh sách đơn hàng của user từ Pancake POS.

---

## Flow xử lý

### 1. Request Flow
```
User truy cập /order-history
    ↓
CartController.orderDetail()
    ↓
Kiểm tra authentication (UserLoginInfo)
    ↓
Lấy thông tin Account từ session
    ↓
Gọi PancakePosService.getAllOrderPancake(phone, 0, 1000)
    ↓
Format dữ liệu (date, money)
    ↓
Add vào Model
    ↓
Return view "order-status"
```

### 2. Controller Logic

**File:** `src/main/java/vn/co/cake/controller/store/CartController.java`
**Method:** `orderDetail()` (line 201-253)

**Các bước xử lý:**

1. **Authentication Check:**
   ```java
   UserLoginInfo loginInfo = getLoginInfo(session);
   if (Objects.isNull(loginInfo)) {
       return "login";  // Redirect về login nếu chưa đăng nhập
   }
   ```

2. **Lấy Account:**
   ```java
   Account account = accountService.getAccount(loginInfo.getId());
   if (account == null) {
       return "login";
   }
   ```

3. **Lấy phone number:**
   ```java
   String phone = account.getPhone();
   if (phone == null) {
       phone = "";  // Tránh NPE
   }
   ```

4. **Gọi API Pancake POS:**
   ```java
   List<OrderPancakeResponse> orderPancake = pancakePosService.getAllOrderPancake(phone, 0, 1000);
   ```
   - `phone`: Số điện thoại của user
   - `0`: pageNumber (trang đầu tiên)
   - `1000`: pageSize (lấy tối đa 1000 orders)

5. **Format dữ liệu:**
   ```java
   // Format date: YYYY-MM-DDTHH:mm:ss → readable format
   if (orderPancakeResponse.getInserted_at() != null) {
       String orderDate = DateUtil.stringToStringFormat(
           orderPancakeResponse.getInserted_at(), 
           DateConst.YYYY_MM_DD_T_HH_MM_SS
       );
       orderPancakeResponse.setInserted_at(orderDate);
   }
   
   // Format money: số → "XXX,XXX VND"
   if (orderPancakeResponse.getMoney_to_collect() != null) {
       String money = BigDecimalUtil.formatMoney(
           orderPancakeResponse.getMoney_to_collect()
       ) + " VND";
       orderPancakeResponse.setMoney_to_collect(money);
   }
   ```

6. **Add vào Model:**
   ```java
   model.addAttribute("orderPancake", orderPancake);
   model.addAttribute("account", account);
   model.addAttribute("isLogin", true);
   ```

---

### 3. Service Logic - PancakePosService.getAllOrderPancake()

**File:** `src/main/java/vn/co/cake/service/external/PancakePosService.java`
**Method:** `getAllOrderPancake(String phone, int pageNumber, int pageSize)` (line 304-373)

**Các bước:**

1. **Validation:**
   ```java
   if (phone == null || phone.trim().isEmpty()) {
       return new ArrayList<>();  // Return empty list nếu phone null
   }
   ```

2. **Lấy PancakeProperties:**
   ```java
   PancakeProperties pancakeProperty = getDefault();
   if (pancakeProperty == null) {
       return new ArrayList<>();
   }
   ```

3. **Build API URL:**
   ```java
   String url = pancakeApiUrl + "/shops/" + pancakeProperty.getShopId() 
       + "/orders?api_key=" + pancakeProperty.getToken() 
       + "&page_size=" + pageSize 
       + "&page_number=" + pageNumber 
       + "&search=" + phone;
   ```

4. **Call API:**
   ```java
   ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
   ```

5. **Parse Response:**
   ```java
   JsonNode rootNode = objectMapper.readTree(response.getBody());
   String variationJson = rootNode.path("data").toString();
   List<OrderPancakeResponse> orders = objectMapper.readValue(
       variationJson, 
       objectMapper.getTypeFactory().constructCollectionType(
           List.class, 
           OrderPancakeResponse.class
       )
   );
   ```

6. **Error Handling:**
   - HttpServerErrorException (5xx) → return empty list
   - HttpClientErrorException (4xx) → return empty list
   - RestClientException → return empty list
   - Exception khác → return empty list

---

### 4. View Logic - order-status.html

**File:** `src/main/resources/templates/order-status.html`

**Cấu trúc hiển thị:**

1. **Kiểm tra dữ liệu:**
   ```html
   <div th:if="${orderPancake == null or orderPancake.isEmpty()}">
       <h3>Bạn chưa có đơn hàng nào</h3>
       <p>Các đơn hàng của bạn sẽ hiển thị tại đây</p>
   </div>
   ```

2. **Hiển thị danh sách orders:**
   ```html
   <div th:if="${orderPancake != null and !orderPancake.isEmpty()}">
       <div th:each="order : ${orderPancake}">
           <!-- Hiển thị từng order -->
       </div>
   </div>
   ```

3. **Thông tin hiển thị cho mỗi order:**
   - **Số thứ tự:** `orderPancake.indexOf(order) + 1`
   - **Tên người nhận:** `order.bill_full_name`
   - **Số điện thoại:** `order.shipping_address.phone_number`
   - **Địa chỉ:** `order.shipping_address.full_address`
   - **Trạng thái:** `order.status_name`
   - **Thời gian đặt:** `order.inserted_at` (đã format)
   - **COD:** `order.money_to_collect` (đã format)
   - **Link tracking:** `order.tracking_link`

---

## DTO Structure

**File:** `src/main/java/vn/co/cake/controller/external/dto/OrderPancakeResponse.java`

**Các fields chính:**
- `bill_full_name`: Tên người nhận
- `shipping_address`: Object chứa địa chỉ
  - `phone_number`
  - `full_address`
- `status_name`: Tên trạng thái đơn hàng
- `inserted_at`: Thời gian tạo đơn
- `money_to_collect`: Số tiền COD
- `tracking_link`: Link theo dõi đơn hàng

---

## Vấn đề tiềm ẩn

### 1. **Pagination không đúng**
- Controller luôn gọi với `pageNumber = 0, pageSize = 1000`
- Nếu user có > 1000 orders, sẽ không hiển thị hết
- **Giải pháp:** Cần implement pagination thực sự

### 2. **Error handling không rõ ràng**
- Nếu API Pancake POS fail, chỉ return empty list
- User không biết lý do tại sao không có đơn hàng
- **Giải pháp:** Thêm message thông báo lỗi

### 3. **Phone number có thể null**
- Code đã handle: `if (phone == null) phone = "";`
- Nhưng nếu phone = "", API Pancake POS sẽ search tất cả orders
- **Giải pháp:** Nên validate phone trước khi gọi API

### 4. **Performance**
- Lấy 1000 orders mỗi lần load page
- Không có caching
- **Giải pháp:** Implement pagination, lazy loading, hoặc caching

### 5. **Data inconsistency**
- Hiển thị orders từ Pancake POS, không phải từ database local
- Nếu order chưa sync thành công (status = SYNC_FAIL), sẽ không hiển thị
- **Giải pháp:** Có thể cần hiển thị cả orders từ local DB

---

## Flow Diagram

```
┌─────────────┐
│   User      │
│  Request    │
│/order-history│
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│ CartController  │
│  orderDetail()  │
└──────┬──────────┘
       │
       ├─► Check Authentication
       │   └─► Fail → Redirect login
       │
       ├─► Get Account from session
       │   └─► Fail → Redirect login
       │
       ├─► Get phone from Account
       │   └─► Null → Set empty string
       │
       ├─► Call PancakePosService
       │   └─► getAllOrderPancake(phone, 0, 1000)
       │
       ├─► Format Data
       │   ├─► Format date
       │   └─► Format money
       │
       └─► Add to Model
           └─► Return "order-status"
                │
                ▼
        ┌───────────────┐
        │ order-status  │
        │    .html      │
        └───────┬───────┘
                │
                ├─► Check orderPancake empty?
                │   ├─► Yes → Show "Chưa có đơn hàng"
                │   └─► No → Loop và hiển thị orders
                │
                └─► Display order details
```

---

## API Call Details

**Endpoint:** `GET /shops/{shopId}/orders`
**Query Parameters:**
- `api_key`: Token từ PancakeProperties
- `page_size`: 1000
- `page_number`: 0
- `search`: Phone number của user

**Response Structure:**
```json
{
  "data": [
    {
      "bill_full_name": "...",
      "shipping_address": {
        "phone_number": "...",
        "full_address": "..."
      },
      "status_name": "...",
      "inserted_at": "...",
      "money_to_collect": 123456,
      "tracking_link": "..."
    }
  ]
}
```

---

## Kết luận

Màn hình order-status hiển thị orders từ Pancake POS API dựa trên phone number của user. Logic khá đơn giản nhưng có một số điểm cần cải thiện về pagination, error handling và performance.

