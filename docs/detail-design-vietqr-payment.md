# Detail Design — Tích hợp thanh toán VietQR (Debase / dragun-cloud)

**Version:** 1.0  
**Ngày:** 2026-06-03  
**Tham chiếu API:** [VietQR Callback](https://api.vietqr.vn/vi/api-vietqr-callback)

---

## 1. Mục tiêu

- Thay **MoMo** (đã tắt) bằng **VietQR** cho phương thức chuyển khoản (`TRANSFER` → `VIETQR`).
- **COD:** giữ nguyên hành vi (tạo đơn → xóa giỏ → sync Pancake).
- **VIETQR:** chỉ tạo đơn + sync Pancake **sau** webhook Transaction Sync thành công; gửi `prepaid` đủ tổng tiền sang Pancake.
- Ghi **payment_transaction_log** để điều tra; màn admin **AM010** xem lịch sử theo mã đơn.

---

## 2. Phạm vi

| Trong phạm vi | Ngoài phạm vi |
|---------------|---------------|
| Host Get Token + Transaction Sync | Sync MID/TID VietQR |
| Client Get Token + Generate QR + Test Callback (sandbox) | MoMo, VNPay |
| `checkout_pending`, `payment_transaction_log` | Export Excel AM010 |
| AM010 read-only | Sửa/xóa log |

---

## 3. Kiến trúc

```
[Store UI] ──POST /api/payment/create──► PaymentCheckoutController
              ├─ COD  → CodCheckoutService → OrderService + Pancake
              └─ VIETQR → VietQrCheckoutService → CheckoutPending + VietQrApiClient

[VietQR] ──POST /vqr/api/token_generate──► VietQrPartnerTokenController
[VietQR] ──POST /vqr/bank/api/transaction-sync──► VietQrTransactionSyncController
              └─ VietQrWebhookService → Order + Payment + Pancake (prepaid)

[Admin AM010] ──GET /AM/AM010──► PaymentTransactionLogQueryService
```

Package: `vn.co.cake.payment.*` — tách khỏi `OrderServiceImpl`.

---

## 4. Luồng nghiệp vụ

### 4.1 COD (không đổi)

1. User chọn COD → `POST /api/payment/create`.
2. `orderService.create` → `deletedCart` → `syncOrderToPancakeAsync` → `Thread.sleep(3000)`.
3. Response: `resultType=ORDER_PLACED`, message như cũ.

### 4.2 VIETQR

1. User chọn VietQR → `POST /api/payment/create`.
2. Sinh `vietqrOrderId` (11 ký tự, cùng format `orders.code`).
3. Lưu `checkout_pending` (cart + địa chỉ JSON).
4. Gọi VietQR: token → `generate-customer` (`qrType=0`, `amount`, `orderId`, `content` ≤23 ký tự).
5. Response: `resultType=QR_PAYMENT`, `qrLink`, `qrCode`, `orderId`, `amount`, `content`.
6. **Không** tạo `orders`, **không** xóa giỏ. Ghi `payment_transaction_log` với `status=INFO` (không phải thanh toán xong).
7. UI poll `GET /api/payment/status/{vietqrOrderId}` — chỉ khi `checkout_pending.status=PAID` (hoặc đã có `orders`) mới coi là xong.
8. VietQR gọi Transaction Sync → xác thực Bearer (partner JWT) → idempotent theo `transactionid`.
9. `orderService.create`, `prepaid = totalAmount + shippingFee`, `payments` SUCCESS, `checkout_pending=PAID`, xóa giỏ, `syncOrderToPancakeAsync`. Log `VIETQR_WEBHOOK_SYNC` với `status=SUCCESS`.

### 4.3 Sandbox test

Sau Generate QR: `POST dev.vietqr.org/vqr/bank/api/test/transaction-callback` với đúng `content` + `amount`.

---

## 5. Cơ sở dữ liệu

### 5.1 `checkout_pending`

| Cột | Kiểu | Mô tả |
|-----|------|--------|
| id | bigserial PK | |
| vietqr_order_id | varchar(13) UNIQUE | Mã gửi VietQR |
| account_id | bigint | |
| amount | numeric(19,2) | Tổng thanh toán |
| content | varchar(23) | Nội dung CK |
| status | varchar(16) | PENDING, PAID, EXPIRED |
| request_json | text | OrderDetailRequest + cart snapshot |
| qr_link | varchar(512) | |
| expires_at | timestamp | |

### 5.2 `payment_transaction_log`

| Cột | Kiểu | Mô tả |
|-----|------|--------|
| event_type | varchar(64) | CHECKOUT_COD, VIETQR_GENERATE_QR, ... |
| status | varchar(16) | SUCCESS, FAILED |
| vietqr_order_id | varchar(13) | |
| order_id | bigint | |
| order_code | varchar(32) | |
| external_txn_id | varchar(64) | |
| error_code, error_message | | |
| request_payload, response_payload | text | Truncate 8KB |

### 5.3 `payments`

Ghi khi VietQR webhook thành công: `payment_method=VIETQR`, `payment_status=SUCCESS`, `transaction_id`.

---

## 6. API nội bộ

| Method | Path | Mô tả |
|--------|------|--------|
| POST | `/api/payment/create` | Checkout COD / VietQR |
| GET | `/api/payment/status/{vietqrOrderId}` | PENDING / PAID / EXPIRED |
| GET | `/payment/vietqr` | Trang hiển thị QR |
| POST | `/vqr/api/token_generate` | Partner — VietQR gọi |
| POST | `/vqr/bank/api/transaction-sync` | Webhook VietQR |

---

## 7. Cấu hình (`application-*.properties`)

```properties
vietqr.enabled=true
vietqr.api.base-url=https://dev.vietqr.org
vietqr.client.username=
vietqr.client.password=
vietqr.partner.username=
vietqr.partner.password=
vietqr.partner.jwt-secret=
vietqr.bank.code=VCB
vietqr.bank.account=
vietqr.bank.holder=
vietqr.checkout.expire-hours=24
```

---

## 8. Bảo mật

- `/vqr/**`: `permitAll`, CSRF ignore.
- Partner token: JWT HS512, TTL 300s.
- Webhook: Bearer validate; không log password/token.
- AM010: `ROLE_ADMIN`, `ROLE_STAFF`.

---

## 9. Pancake

`MainOrderRequest`: set `prepaid` khi `order.prepaid > 0` (sau thanh toán VietQR).

---

## 10. Admin AM010

- Search: mã đơn, mã VietQR, status, event_type, ngày.
- List + modal chi tiết payload.
- Path: `/AM/AM010`, detail: `/AM/AM010/detail/{id}`.

---

## 11. File code chính

| Loại | Path |
|------|------|
| Constants | `payment/PaymentConstants.java` |
| Config | `payment/config/VietQrProperties.java` |
| COD | `payment/service/CodCheckoutService.java` |
| VietQR | `payment/service/VietQrCheckoutService.java`, `VietQrWebhookService.java` |
| Client | `payment/vietqr/VietQrApiClient.java` |
| Host | `payment/api/VietQrPartnerTokenController.java`, `VietQrTransactionSyncController.java` |
| Store | `payment/api/PaymentCheckoutController.java` |
| Admin | `controller/AM010/AM010Controller.java` |

---

## 12. Xóa MoMo

- `MomoController`, `MoMoCallbackController`, `MoMoPaymentService*`, `PaymentRequest.java`
- UI radio MoMo; ảnh QR tĩnh thủ công

---

## 13. Test checklist

- [ ] COD đặt hàng + Pancake như trước
- [ ] VietQR Generate QR + Test Callback → 1 đơn, prepaid Pancake
- [ ] Webhook retry → không duplicate order
- [ ] AM010 search theo mã đơn
