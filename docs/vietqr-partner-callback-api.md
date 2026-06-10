# Hướng dẫn API Callback VietQR — Debase (Production)

**Đối tượng:** Đội kỹ thuật VietQR Partner  
**Merchant:** Debase — https://debase.vn  
**Phiên bản tài liệu:** 1.0  
**Ngày:** 2026-06-03  

Tài liệu mô tả 2 API mà **VietQR gọi vào hệ thống Debase** sau khi khách hàng chuyển khoản thành công qua mã VietQR.

---

## 1. Tổng quan luồng

```
Khách quét QR & chuyển khoản
         │
         ▼
   VietQR phát hiện giao dịch
         │
         ├─① POST /vqr/api/token_generate          (Basic Auth — lấy JWT)
         │
         └─② POST /vqr/bank/api/transaction-sync  (Bearer JWT — báo thanh toán)
                    │
                    ▼
            Debase tạo đơn hàng, xác nhận thanh toán
```

- **Bước ①** chỉ dùng để xác thực và cấp token tạm (JWT, TTL **300 giây**).
- **Bước ②** mới chứa dữ liệu giao dịch và kích hoạt tạo đơn trên hệ thống Debase.
- Hai bước **bắt buộc theo thứ tự**: gọi `transaction-sync` phải kèm `Bearer` token hợp lệ từ bước ①.

---

## 2. Môi trường Production

| Mục | Giá trị |
|-----|---------|
| Base URL | `https://debase.vn` |
| API VietQR (phía Debase gọi ra) | `https://api.vietqr.org` |
| Giao thức | HTTPS (TLS), port 443 |
| CSRF | Không áp dụng cho `/vqr/**` |
| Content-Type | `application/json` |

---

## 3. Thông tin Partner cần cấu hình

### 3.1 Cung cấp cho VietQR (đăng ký trên cổng Partner)

| Thông tin | Mô tả |
|-----------|--------|
| **Partner Username** | Tài khoản partner để gọi API ① |
| **Partner Password** | Mật khẩu partner (dùng Basic Auth) |
| **Token URL** | `https://debase.vn/vqr/api/token_generate` |
| **Transaction Sync URL** | `https://debase.vn/vqr/bank/api/transaction-sync` |

> **Lưu ý:** Username và password partner phải **khớp chính xác** với cấu hình trên server Debase.  
> VietQR **không** cần biết `jwt-secret` — secret này chỉ dùng nội bộ trên server Debase để ký/verify JWT.

### 3.2 Chỉ cấu hình trên server Debase (không gửi cho VietQR)

| Cấu hình | Mục đích |
|----------|----------|
| `jwt-secret` | Ký JWT trả về ở API ①; verify Bearer ở API ② |

---

## 4. API ① — Lấy Access Token

### Request

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `https://debase.vn/vqr/api/token_generate` |
| **Header** | `Authorization: Basic {Base64(username:password)}` |
| **Body** | Không bắt buộc (có thể để trống) |

**Ví dụ header:**

```http
POST /vqr/api/token_generate HTTP/1.1
Host: debase.vn
Authorization: Basic ZGViYXNlMTk5NTpEZWJhc2UxOTk1QEB=
Content-Type: application/json
```

(`ZGViYXNlMTk5NTpEZWJhc2UxOTk1QEB=` = Base64 của `username:password` — giá trị thực do hai bên thống nhất.)

### Response thành công — HTTP 200

```json
{
  "access_token": "eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 300
}
```

| Field | Mô tả |
|-------|--------|
| `access_token` | JWT dùng cho API ② |
| `token_type` | Luôn là `Bearer` |
| `expires_in` | Thời gian sống (giây), mặc định **300** |

### Response lỗi — HTTP 400

```json
{
  "status": "FAILED",
  "message": "UNAUTHORIZED"
}
```

**Nguyên nhân thường gặp:** Sai username/password Basic Auth.

---

## 5. API ② — Transaction Sync (Webhook thanh toán)

### Request

| | |
|---|---|
| **Method** | `POST` |
| **URL** | `https://debase.vn/vqr/bank/api/transaction-sync` |
| **Header** | `Authorization: Bearer {access_token}` |
| **Header** | `Content-Type: application/json` |
| **Body** | JSON — xem bảng dưới |

**Ví dụ header:**

```http
POST /vqr/bank/api/transaction-sync HTTP/1.1
Host: debase.vn
Authorization: Bearer eyJhbGciOiJIUzUxMiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json
```

### Request body

| Field | Kiểu | Bắt buộc | Mô tả |
|-------|------|----------|--------|
| `orderId` | string | Có | Mã đơn VietQR (tối đa 13 ký tự), trùng mã gửi khi generate QR |
| `amount` | number | Có | Số tiền chuyển khoản (VND), phải **khớp** số tiền đơn chờ thanh toán |
| `transType` | string | Có | Loại giao dịch — phải là **`C`** (Credit / tiền vào) |
| `content` | string | Có | Nội dung chuyển khoản (tối đa 23 ký tự) |
| `transactionid` | string | Khuyến nghị | Mã giao dịch duy nhất từ ngân hàng/VietQR (dùng chống trùng) |
| `transactiontime` | number | Không | Unix timestamp giao dịch |
| `bankaccount` | string | Không | Số tài khoản nhận |
| `referencenumber` | string | Không | Mã tham chiếu ngân hàng |
| `sign` | string | Không | Chữ ký (nếu có theo spec VietQR) |

**Ví dụ body:**

```json
{
  "bankaccount": "8897487517",
  "amount": 142500,
  "transType": "C",
  "content": "DH12345678901",
  "transactionid": "FT25234000123456",
  "transactiontime": 1781001421,
  "referencenumber": "REF123456",
  "orderId": "12345678901",
  "sign": ""
}
```

### Response thành công — HTTP 200

```json
{
  "status": "SUCCESS",
  "message": ""
}
```

Sau khi nhận `SUCCESS`, Debase sẽ:

1. Xác nhận `checkout_pending` → `PAID`
2. Tạo đơn hàng (`orders`)
3. Ghi nhận thanh toán (`payments`)
4. Xóa giỏ hàng khách
5. Đồng bộ Pancake POS

### Response lỗi

| HTTP | Body ví dụ | Ý nghĩa |
|------|------------|---------|
| 401 | `{ "status": "FAILED", "message": "INVALID_TOKEN" }` | Bearer token hết hạn hoặc không hợp lệ |
| 400 | `{ "status": "FAILED", "message": "..." }` | Lỗi nghiệp vụ (xem bảng dưới) |
| 500 | `{ "status": "FAILED", "message": "..." }` | Lỗi hệ thống |

**Một số message lỗi nghiệp vụ:**

| Message | Nguyên nhân |
|---------|-------------|
| `Checkout pending not found` | Không tìm thấy đơn chờ với `orderId` hoặc đã hết hạn |
| `Invalid transaction type` | `transType` khác `C` |
| `Amount mismatch` | `amount` không khớp tổng tiền đơn |

### Idempotent (gọi lại an toàn)

- Nếu đơn đã `PAID`, webhook gọi lại với cùng `orderId` → Debase **bỏ qua**, vẫn trả `SUCCESS`.
- Nếu trùng `transactionid` đã xử lý thành công → bỏ qua, không tạo đơn trùng.

---

## 6. Sơ đồ tuần tự

```mermaid
sequenceDiagram
    participant K as Khách hàng
    participant D as Debase
    participant V as VietQR
    participant B as Ngân hàng

    K->>D: Đặt hàng VietQR
    D->>V: Generate QR (api.vietqr.org)
    V-->>D: qrLink, orderId, amount
    D-->>K: Hiển thị QR
    K->>B: Chuyển khoản
    B->>V: Thông báo giao dịch
    V->>D: POST /vqr/api/token_generate (Basic)
    D-->>V: access_token (JWT, 300s)
    V->>D: POST /vqr/bank/api/transaction-sync (Bearer)
    D-->>V: status SUCCESS
    D-->>K: Poll trạng thái → Thanh toán thành công
```

---

## 7. Kiểm thử kết nối (curl)

### 7.1 Test API Token

```bash
curl -i -X POST "https://debase.vn/vqr/api/token_generate" \
  -H "Authorization: Basic $(printf '%s' 'PARTNER_USERNAME:PARTNER_PASSWORD' | base64 -w0)"
```

Kỳ vọng: HTTP `200` và có `access_token`.

### 7.2 Test API Transaction Sync

```bash
TOKEN="<access_token_từ_bước_7.1>"

curl -i -X POST "https://debase.vn/vqr/bank/api/transaction-sync" \
  -H "Authorization: Bearer ${TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "MADON_TEST",
    "amount": 142500,
    "transType": "C",
    "content": "NOIDUNGCK",
    "transactionid": "TEST-TXN-001"
  }'
```

> **Lưu ý:** `orderId` và `amount` phải tồn tại trong `checkout_pending` (PENDING) trên hệ thống Debase — tức khách đã bắt đầu checkout VietQR trước đó. Gọi sync với mã đơn ngẫu nhiên sẽ trả lỗi `Checkout pending not found`.

---

## 8. Phân biệt credential (tránh nhầm)

| Credential | Hướng | Dùng cho |
|------------|--------|----------|
| **Partner** (`username` / `password`) | VietQR → Debase | API ① `token_generate` (Basic Auth) |
| **Client** (`customer-...`) | Debase → VietQR | Tạo mã QR trên `api.vietqr.org` |

Hai loại credential **khác nhau**, không thay thế cho nhau.

---

## 9. Checklist triển khai Production

- [ ] Đăng ký URL Token: `https://debase.vn/vqr/api/token_generate`
- [ ] Đăng ký URL Transaction Sync: `https://debase.vn/vqr/bank/api/transaction-sync`
- [ ] Cấu hình Partner username/password (khớp server Debase)
- [ ] Xác nhận HTTPS và firewall cho phép VietQR gọi vào
- [ ] Test bước ① trả `access_token`
- [ ] Test thanh toán thật nhỏ → bước ② trả `SUCCESS` → đơn tạo trên Debase

---

## 10. Tham chiếu kỹ thuật (phía Debase)

| Thành phần | File / path |
|------------|-------------|
| API Token | `VietQrPartnerTokenController` — `POST /vqr/api/token_generate` |
| API Webhook | `VietQrTransactionSyncController` — `POST /vqr/bank/api/transaction-sync` |
| Xử lý thanh toán | `VietQrWebhookService.confirmPayment()` |
| Thiết kế chi tiết | `docs/detail-design-vietqr-payment.md` |
| Spec VietQR | https://api.vietqr.vn/vi/api-vietqr-callback |

---

## 11. Liên hệ

Khi gặp lỗi callback, vui lòng cung cấp:

- Thời gian gọi API (UTC+7)
- `orderId`
- `transactionid`
- HTTP status + response body
- Request body (ẩn thông tin nhạy cảm nếu có)

Đội kỹ thuật Debase tra cứu log sự kiện `VIETQR_PARTNER_GET_TOKEN` và `VIETQR_WEBHOOK_SYNC` trên màn hình admin AM010.
