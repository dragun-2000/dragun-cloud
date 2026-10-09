# DOC-ICD-01 — VietQR Callback API (Interface Control Document)

| Mục | Giá trị |
|-----|---------|
| **Mã tài liệu** | DOC-ICD-01 |
| **Phiên bản** | 1.0 |
| **Ngày** | 2026-10-09 |
| **Đối tượng đọc** | Đội kỹ thuật VietQR Partner, SA Debase |
| **Merchant** | De Basé — https://debase.vn |
| **Liên quan** | BR-07, UC-04, DOC-DEV-02, DOC-UAT-05 (C2–C3) |

---

Tài liệu mô tả **2 API VietQR gọi vào Debase** sau khi khách chuyển khoản thành công qua mã VietQR.

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
            Debase tạo/xác nhận đơn hàng
```

- Bước ①: cấp JWT tạm (TTL **300 giây**).  
- Bước ②: báo giao dịch; bắt buộc Bearer từ bước ①.  
- Chi tiết thiết kế nội bộ: [DOC-DEV-02](../dev/DOC-DEV-02-vietqr-detail-design.md).

## 2. Môi trường Production

| Mục | Giá trị |
|-----|---------|
| Base URL | `https://debase.vn` |
| API VietQR (Debase gọi ra) | `https://api.vietqr.org` |
| Giao thức | HTTPS |
| CSRF | Không áp dụng cho `/vqr/**` |
| Content-Type | `application/json` |

## 3. Cấu hình Partner

### Cung cấp cho VietQR

| Thông tin | Mô tả |
|-----------|--------|
| Partner Username / Password | Basic Auth API ① |
| Token URL | `https://debase.vn/vqr/api/token_generate` |
| Transaction Sync URL | `https://debase.vn/vqr/bank/api/transaction-sync` |

### Chỉ trên server Debase

| Cấu hình | Mục đích |
|----------|----------|
| `jwt-secret` | Ký/verify JWT — **không** gửi cho VietQR |

## 4. API ① — Token

| | |
|--|--|
| Method | `POST` |
| URL | `https://debase.vn/vqr/api/token_generate` |
| Auth | `Authorization: Basic {Base64(username:password)}` |

**Response 200**

```json
{
  "access_token": "eyJ...",
  "token_type": "Bearer",
  "expires_in": 300
}
```

**Response 400 (auth sai):** `{ "status": "FAILED", "message": "UNAUTHORIZED" }`

## 5. API ② — Transaction Sync

| | |
|--|--|
| Method | `POST` |
| URL | `https://debase.vn/vqr/bank/api/transaction-sync` |
| Auth | `Authorization: Bearer {access_token}` |

### Request body (chính)

| Field | Bắt buộc | Mô tả |
|-------|----------|--------|
| `orderId` | Có | Mã đơn VietQR (khớp lúc generate QR) |
| `amount` | Có | Số tiền VND khớp đơn chờ |
| `transType` | Có | Phải là `C` (Credit) |
| `content` | Có | Nội dung CK |
| `transactionid` | Khuyến nghị | Chống trùng |

**Response 200:** `{ "status": "SUCCESS", "message": "" }`

Sau SUCCESS, Debase: xác nhận pending → tạo đơn → ghi payment → xử lý giỏ → đồng bộ Pancake (theo cấu hình).

**Lỗi thường gặp:** `INVALID_TOKEN`, `Checkout pending not found`, `Invalid transaction type`, `Amount mismatch`.

**Idempotent:** đã PAID / trùng `transactionid` → bỏ qua, vẫn `SUCCESS`.

## 6. Phân biệt credential

| Credential | Hướng | Dùng cho |
|------------|--------|----------|
| Partner username/password | VietQR → Debase | API ① |
| Client VietQR (customer) | Debase → VietQR | Generate QR |

## 7. Checklist kết nối

- [ ] Đăng ký Token URL & Sync URL trên cổng Partner  
- [ ] Partner credentials khớp server Debase  
- [ ] Test ① có `access_token`  
- [ ] Test thanh toán nhỏ → ② `SUCCESS` → đơn trên Debase  

## 8. Liên hệ sự cố

Cung cấp: thời gian (UTC+7), `orderId`, `transactionid`, HTTP status + body. Tra cứu AM010 sự kiện `VIETQR_PARTNER_GET_TOKEN`, `VIETQR_WEBHOOK_SYNC`.

Spec VietQR tham chiếu: https://api.vietqr.vn/vi/api-vietqr-callback
