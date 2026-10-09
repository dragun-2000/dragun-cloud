# Phân tích lỗi 443 – App Docker gọi Pancake POS timeout, Terminal (curl) thành công

## 1. Tóm tắt hiện tượng

| Nguồn gọi | URL / Hành vi | Kết quả |
|-----------|----------------|--------|
| **Terminal (curl)** | `GET https://pos.pages.fm/api/v1/shops/2447703/orders?api_key=...` | HTTP 200, body ~85KB |
| **App trong Docker** | Cùng host, cùng endpoint (GET/POST qua `pancakeRestTemplate`) | Lỗi 443 / timeout ("The target server failed to respond") |

→ Mạng từ container ra internet **không** bị chặn (curl trong container đã test OK). Vấn đề nằm ở **phía client Java** trong container.

### Lỗi NoHttpResponseException ("The target server failed to respond")

- **Ý nghĩa:** Kết nối TCP + TLS đã thiết lập xong, nhưng **server (hoặc proxy/Cloudflare) đóng connection mà không gửi HTTP response**. Khác với ConnectTimeout (chưa kịp nối) hay SocketTimeout (không nhận data kịp).
- **Thường gặp:** Server/proxy đóng kết nối tạm thời (tải cao, keep-alive timeout phía server), hoặc WAF/proxy không thích request (User-Agent, header). Retry với connection mới thường xử lý được.
- **Đã xử lý trong code:** Retry tối đa 3 lần (cấu hình được), delay 2s giữa các lần; gửi header `User-Agent: DebaseApp/1.0 (Java)`; message lỗi lưu DB rõ là "Server closed connection without response" khi root cause là NoHttpResponseException; Job002 vẫn retry các đơn SYNC_FAIL.

---

## 2. Luồng gọi trong code

### 2.1 RestTemplate dùng cho Pancake

- **Bean:** `pancakeRestTemplate` (`PancakeRestTemplateConfig`), `@Primary`.
- **Inject:** `PancakePosService` dùng `@Qualifier("pancakeRestTemplate") RestTemplate`.
- **Stack:**  
  `RestTemplate` → `HttpComponentsClientHttpRequestFactory` → **Apache HttpClient 4.5.8** (không dùng `HttpURLConnection`).

### 2.2 Các điểm gọi API Pancake

| Method | HTTP | URL pattern | Ghi chú |
|--------|------|-------------|--------|
| `createOrder()` | POST | `{pancakeApiUrl}/shops/{shopId}/orders?api_key={token}` | Sync đơn lên Pancake – thường là chỗ báo lỗi 443 |
| `getAllOrderPancake()` | GET | `.../orders?api_key=...&page_size=...&page_number=...&search={phone}` | Lấy danh sách đơn (giống kiểu curl GET thành công) |
| `getAllProductPancake()` | GET | `.../products/variations?api_key=...&page_number=...` | Lấy biến thể sản phẩm |
| `createProduct()` | POST | `.../products?api_key=...` | Tạo sản phẩm |

Cần xác định: **chỉ POST (createOrder) lỗi hay cả GET từ app cũng lỗi?** Điều này tách được hai nhóm nguyên nhân (xem mục 4).

### 2.3 Cấu hình HttpClient (Pancake)

- **Timeout:**  
  - `connectTimeout`: 45s (properties)  
  - `socketTimeout` (read): 60s  
  - `connectionRequestTimeout`: 10s (lấy connection từ pool)
- **Connection:**  
  - `NoConnectionReuseStrategy` → mỗi request một connection mới, không reuse.  
  - `disableConnectionState()` → không giữ connection state.  
  - `disableAutomaticRetries()` → không retry tự động.
- **Request:**  
  - Header `Connection: close` được set trong `createOrder()`.

So với curl: curl mỗi lần chạy cũng là một connection mới. Về mặt “một request = một connection mới” thì đã gần giống curl.

---

## 3. Khác biệt có thể gây “chỉ Java lỗi, curl không”

### 3.1 DNS

- **Curl:** dùng resolver của hệ thống (trong container = resolver của Docker/container).
- **JVM:** có cache DNS riêng (mặc định cache vĩnh viễn khi bảo mật manager cho phép).  
  Nếu lần đầu resolve sai/chậm hoặc sau khi server đổi IP, JVM có thể tiếp tục dùng địa chỉ cũ → connect sai host hoặc timeout.  
  **Cách kiểm tra:** giảm cache DNS JVM (ví dụ `-Dnetworkaddress.cache.ttl=0` hoặc `60`) hoặc test với URL dạng IP thay vì hostname.

### 3.2 IPv4 vs IPv6

- **Curl (trong container):** output thường thấy `IPv4: 113.20.119.3, 113.20.119.56` và `IPv6: (none)` → kết nối đi bằng IPv4.
- **JVM:** mặc định có thể thử IPv6 trước. Trong một số môi trường Docker, IPv6 không được cấu hình đúng hoặc không đi ra internet → connect treo/timeout.  
  **Đã thử:** thêm `-Djava.net.preferIPv4Stack=true` trong `JAVA_TOOL_OPTIONS` (docker-compose) để ép JVM chỉ dùng IPv4.

### 3.3 TLS / SSL

- **Curl:** OpenSSL (trong container).
- **Java:** JSSE (cacerts trong JRE).  
  Cùng server (pos.pages.fm) nên certificate chain về cơ bản giống nhau. Khả năng khác:  
  - cipher suite hoặc TLS version JVM chọn bị proxy/firewall chặn hoặc xử lý chậm;  
  - hoặc vấn đề chỉ xảy ra khi kết hợp với connection reuse/pool (đã tắt reuse).  
  Nếu sau khi ép IPv4 và DNS vẫn lỗi, có thể bật wire log (xem 5.2) để xem TLS handshake có hoàn thành không.

### 3.4 Connection pool (Apache HttpClient)

- **Curl:** không pool, mỗi lần một connection.
- **HttpClient:** vẫn có pool (mặc định ví dụ `maxConnPerRoute=2`).  
  Với `NoConnectionReuseStrategy`, connection sau khi dùng xong bị đóng, không tái sử dụng. Tuy nhiên khi nhiều request đồng thời (ví dụ nhiều đơn sync cùng lúc), có thể có blocking “chờ connection từ pool” (bị giới hạn bởi `connectionRequestTimeout` 10s) hoặc tạo nhiều connection đồng thời.  
  Nếu nghi ngờ pool: có thể set `maxConnPerRoute` và `maxConnTotal` rõ ràng hoặc tạm thời dùng `SimpleClientHttpRequestFactory` (HttpURLConnection, không pool) để so sánh.

### 3.5 Thời gian xử lý (POST vs GET)

- **GET list orders (curl):** response ~85KB, server trả nhanh.
- **POST create order:** server phải xử lý tạo đơn, có thể chậm hơn; response cũng có thể lớn hơn.  
  Nếu **chỉ POST** bị timeout có thể là **read timeout** (60s không đủ) hoặc server trả chậm. Nếu **cả GET từ app** cũng timeout thì gần như chắc chắn là **connect/timeout ở tầng kết nối (443)** chứ không phải do “POST đặc biệt”.

### 3.6 Async / thread pool

- `syncOrderToPancakeAsync()` gọi `createOrder()` trong executor thread.  
  Không có lý do đặc biệt khiến “chỉ async” bị chặn, trừ khi executor bị giới hạn (ví dụ queue đầy, thread ít) hoặc có shared resource (ví dụ connection pool) bị tranh chấp. Cần xem log có nhiều request Pancake cùng lúc không.

---

## 4. Cây quyết định nguyên nhân (nên kiểm tra theo thứ tự)

1. **GET từ app (Java) có chạy được không?**  
   - Nếu **GET từ app cũng timeout 443** → vấn đề là **kết nối outbound HTTPS từ JVM** (DNS, IPv6, TLS, hoặc pool). Ưu tiên: IPv4, DNS cache, sau đó wire log / thử HttpURLConnection.  
   - Nếu **GET từ app OK, chỉ POST (createOrder) lỗi** → nghiêng về **read timeout hoặc server xử lý POST chậm**; có thể tăng read timeout hoặc kiểm tra phía Pancake.

2. **Trong container, curl và app có cùng môi trường không?**  
   - Cùng container, cùng network: nếu curl OK mà Java vẫn lỗi thì không phải firewall container, mà là **client Java** (JVM + HttpClient).

3. **Log lỗi chi tiết:**  
   - Đã thêm log in **root cause** (ví dụ `ConnectTimeoutException` vs `SocketTimeoutException`).  
   - `ConnectTimeoutException` → không thiết lập được TCP/TLS trong 45s (DNS, IPv6, firewall, TLS).  
   - `SocketTimeoutException` → đã connect, nhưng không nhận data trong 60s (server chậm hoặc read timeout cần tăng).

---

## 5. Các bước chẩn đoán đã làm / đề xuất

### 5.1 Đã làm

- Tăng connect timeout 15s → 45s (config).
- Set `Connection: close`, `NoConnectionReuseStrategy`, `disableConnectionState`, `disableAutomaticRetries`.
- Thêm `-Djava.net.preferIPv4Stack=true` (docker-compose).
- Log exception type (root cause) khi lỗi 443.

### 5.2 Đề xuất tiếp theo

1. **Bật diagnostic GET trên startup**  
   - Set `pancake.pos.diagnostic-on-startup=true` (ví dụ trong `application-production.properties` hoặc env).  
   - Class `PancakePosDiagnosticRunner` sẽ gọi **một GET** tới cùng URL kiểu curl (list orders, page_size=1) bằng **cùng `pancakeRestTemplate`** ngay sau khi app ready.  
   - Log: `Pancake POS diagnostic: SUCCESS ...` hoặc `Pancake POS diagnostic: FAILED ... [ConnectTimeoutException] ...`.  
   - Nếu GET diagnostic **thành công** → kết nối từ JVM ra pos.pages.fm **có thể** thiết lập được; khi đó lỗi 443 thực tế có thể chỉ xảy ra với **POST createOrder** (timeout đọc response hoặc tải server).  
   - Nếu GET diagnostic **cũng timeout** → xử lý tiếp theo tập trung vào DNS (cache, thử IP), TLS (wire log), hoặc thử HttpURLConnection.

2. **Bật wire log Apache HttpClient (tạm thời)**  
   - Log level `DEBUG` cho `org.apache.http.wire`.  
   - Xem request/response raw và thời điểm dừng (sau TLS handshake hay sau khi gửi body, v.v.) để biết lỗi ở bước nào.

3. **Thử dùng HttpURLConnection**  
   - Tạo một `RestTemplate` với `SimpleClientHttpRequestFactory` (không dùng Apache HttpClient), cùng URL và timeout.  
   - Nếu RestTemplate này **OK** mà HttpClient vẫn lỗi → nghi ngờ nằm ở Apache HttpClient (pool, TLS implementation, v.v.).

4. **Giảm DNS cache JVM**  
   - Thêm `-Dnetworkaddress.cache.ttl=60` (hoặc `0` khi debug) trong `JAVA_TOOL_OPTIONS` để tránh JVM dùng địa chỉ DNS cũ quá lâu.

---

## 6. Kết luận tạm thời

- **Chỗ “bị chặn” nhiều khả năng không phải mạng container hay firewall đơn thuần**, vì curl trong cùng container vẫn thành công.
- **Hướng nghi ngờ chính:**  
  - Cách **JVM/HttpClient** thiết lập kết nối (DNS, IPv4/IPv6, TLS) hoặc  
  - **Chỉ** request **POST createOrder** bị chậm (read timeout / server xử lý lâu).
- **Cách xác định nhanh:** bật diagnostic GET trên startup; dựa vào kết quả (GET OK vs GET cũng 443) để áp dụng đúng nhánh trong mục 4 và 5.

File này nên được cập nhật sau khi chạy diagnostic và có thêm log (exception type, wire log) để khoanh vùng chính xác điểm lỗi.
