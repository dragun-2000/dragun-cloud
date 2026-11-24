# 07 — Business Rules (as implemented)

Baseline `a0515d8`. Every rule below must be preserved exactly by any refactor. Flow references → `06`. Paths relative to `src/main/java/vn/co/cake/`.

## 1. State machines

### 1.1 `Order.status` (String; values in `enums/OrderStatus`, `constants/OrderConstants`)

| From | To | Set by |
|---|---|---|
| — | `NEW` | COD create (`OrderServiceImpl:164`) |
| — | `AWAITING_PAYMENT` | VietQR start (`VietQrCheckoutService:92`), VNPay start (`VnpayCheckoutService:95`) |
| AWAITING_PAYMENT / CANCELLED(!) | `PENDING_SYNC` | VietQR webhook (`VietQrWebhookService:157`), VNPay applyPaid (`VnpayPaymentResultService:99`) |
| any paid flow failure | `PAYMENT_RECEIVED_UNFULFILLABLE` | `VietQrWebhookService:216`, `VnpayPaymentResultService:87` |
| PAYMENT_RECEIVED_UNFULFILLABLE | `PENDING_SYNC` | AM006 retry (`AM006Controller:140`) |
| PENDING_SYNC / SYNC_FAIL / any with pancakeOrderId | `NEW` | Pancake create success / idempotent branch (`PancakePosService:207, 536`) |
| PENDING_SYNC / NEW / SYNC_FAIL | `SYNC_FAIL` (+countError) | Pancake create failure (`PancakePosService:646`) |
| AWAITING_PAYMENT | `CANCELLED` | VietQR expiry (Job004:95, `VietQrCheckoutService:269`), VNPay unpaid (`VnpayPaymentResultService:117`) |
| NEW | `CANCELLED` | Customer cancel (`CustomerOrderCancelService:82`) |

`deleted=true` is set independently (AM006 delete). Job003 / order-history refresh / webhook **never** change status.

### 1.2 `CheckoutPending.status` (VietQR): PENDING → PAID | PAID_ISSUE | EXPIRED (EXPIRED → PAID possible via late webhook).
### 1.3 `InventoryReservation.status`: HELD → CONFIRMED → CONSUMED; HELD → RELEASED; RELEASED → HELD (reuse in `ensureConfirmedForPaidOrder`). Unique (order_id, variation_id).
### 1.4 `VnpayPayment.status`: PENDING → PAID | PAID_ISSUE | FAILED | EXPIRED. Final for IPN = {PAID, PAID_ISSUE, FAILED}; **EXPIRED is not final**.
### 1.5 Pancake fulfillment mirror (`enums/PancakeFulfillmentStatus`)

| Code | Name | Hourly-polled | Customer cancelMode |
|---|---|---|---|
| 0 | Mới (FULFILLMENT_NEW) | yes | SELF |
| 1 | Đã xác nhận | yes | CONTACT |
| 2 | Đã gửi hàng | yes | CONTACT |
| 3 | Đã nhận (terminal) | no | CONTACT |
| 6 | Đã hủy (terminal) | no | NONE |
| 7 | Đang hoàn | no | CONTACT |
| 8 | Đang đóng hàng | yes | CONTACT |
| 9 | Chờ chuyển hàng | yes | CONTACT |
| 11 | Chờ hàng | no | CONTACT |

Resolution: code first, then accent-stripped name/aliases; "Trạng thái #n" treated as blank; unknown stored raw. cancelMode: lifecycle ≠ NEW ⇒ NONE; blank code+name ⇒ SELF. Display fallback by lifecycle status when unresolved (fixed Vietnamese labels).

## 2. Pricing & money

| ID | Rule | Location |
|---|---|---|
| BR-P1 | Selling unit price = product.finalPrice if > 0, else product.price if > 0, else variation.retailPrice, else 0 | `utils/OrderPricingUtil.java:21` |
| BR-P2 | Line total = unit × qty, truncated with `longValue()` per line; qty ≤ 0 and unknown variations skipped | `OrderServiceImpl:244-272` |
| BR-P3 | Shipping fee = `SHIPPING_FEE` voucher fee when subtotal < 2,000,000 (`FREE_SHIPPING_THRESHOLD`), else 0 | `OrderServiceImpl:157-161`, `CartController:242` |
| BR-P4 | `Order.totalAmount` = subtotal − 0 (voucher discount **not** applied; `discountPrice` hardcoded 0) | `OrderServiceImpl:155,166` ✔ |
| BR-P5 | Grand total for payment = totalAmount + shippingFee (VietQR, VNPay) | checkout services |
| BR-P6 | COD limit: subtotal + shipping − voucher% (FLOOR) ≤ 1,500,000 | `PaymentCheckoutController:168-176`, `PaymentOrderTotalCalculator:39-50` |
| BR-P7 | Product discount: discountAmount = price × discount/100 (HALF_UP); finalPrice = price − discountAmount; field `discountPrice` stores the **amount** | `entity/Product.java:114-124,173-183` |
| BR-P8 | AM008 discount: 0 < d < 100; products with price ≤ 0/null silently skipped | `ProductServiceImpl:282-316` |
| BR-P9 | Job01 (every minute): if `DISCOUNT_PRICE` voucher has both dates and today (JVM zone) is outside [start, end] inclusive ⇒ reset every product with discount > 0 (incl. deleted) | `Job01:31-49`, `ProductServiceImpl:266-279` |
| BR-P10 | VietQR amount match: round HALF_UP to 0 dp, exact equality | `VietQrWebhookService:249-258` |
| BR-P11 | VNPay `vnp_Amount` = VND × 100; IPN amount must match | `VnpayApiClient`, `VnpayFormats.amountMatches` |
| BR-P12 | Pancake payload: `is_free_shipping=true`, shipping fee in note text, `prepaid` only if > 0, `discount` = Σ line discounts, item weight 200 | `controller/external/dto/MainOrderRequest.java:37-119` |
| BR-P13 | Order history paid amount: COD ⇒ 0; online ⇒ prepaid ∨ total, capped at total | `CartController:332-374` |

## 3. Orders, stock & payments

| ID | Rule |
|---|---|
| BR-O1 | Order code = `yyMMdd` + zero-padded daily sequence (`order_sequence`), VNPay txnRef = order code |
| BR-O2 | Stock pre-check at order creation: `remainQuantity − softHeld < qty` ⇒ reject (sold out) |
| BR-O3 | Reservation locks variations `PESSIMISTIC_WRITE` in ascending id order and **hard-deducts** `remainQuantity` and `actualRemainQuantity` at HELD time (`stockDeducted=true`) |
| BR-O4 | Reservation expiry: COD now+10 min (immediately confirmed); VietQR = checkout expiry (default 5 min); VNPay = expire (15) + grace (5) + 60 min; AM006 recreate now+10 min |
| BR-O5 | Paid ⇒ `ensureConfirmedForPaidOrder` in its own transaction; failure ⇒ PAYMENT_RECEIVED_UNFULFILLABLE + admin alert (subject always `[VIETQR]`) |
| BR-O6 | Pancake create success ⇒ CONFIRMED → CONSUMED (except idempotent branch) |
| BR-O7 | Pancake retry: SYNC_FAIL and countError < 25 (`MAX_PANCAKE_SYNC_RETRIES`), every 30 min; admin retry has no cap |
| BR-O8 | Customer may cancel only own, non-deleted, status NEW, with pancakeOrderId, and live Pancake status "new" (code 0) |
| BR-O9 | Customer order history is selected **by account phone**, excluding AWAITING_PAYMENT and CANCELLED-without-pancakeOrderId |
| BR-O10 | Cart is cleared on: COD success; VietQR paid/paid-issue; VNPay applyPaid (before stock confirmation) |
| BR-O11 | Webhook/daily sync set stock as absolute Pancake values; product stock = Σ variation remainQuantity for the pancakeProductId |
| BR-O12 | VietQR idempotency: pending PAID/PAID_ISSUE, or existing SUCCESS log for transactionId (unless still PENDING); transType must be "C" |
| BR-O13 | VNPay IPN response codes: 97 bad signature, 01 not found/tmn mismatch, 04 amount, 02 already final, 99 exception, 00 OK; paid = 00/00 or any 07 (07 ⇒ PAID_ISSUE) |
| BR-O14 | VNPay give-up: expiresAt + `query-dr-give-up-minutes` (default 5) ⇒ EXPIRED (TIMEOUT); QueryDR amount mismatch ⇒ stays PENDING |
| BR-O15 | Pilot gating: visibleToAll ⇒ everyone; else per-session unlock with BCrypt password; VNPay also needs `vnpay_settings.enabled` |

## 4. Accounts & security

| ID | Rule |
|---|---|
| BR-A1 | Phone regex `^(0|84|\+84)(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])[0-9]{7}$` after stripping spaces/dashes/parens; normalized to 10 chars `0xxxxxxxxx` |
| BR-A2 | Customer uniqueness: by normalized phone among ROLE_USER (message constant named EMAIL_ALREADY_EXISTS) |
| BR-A3 | Height 120–250 cm, weight 30–150 kg, both nullable |
| BR-A4 | OTP: 6 digits, 5-minute validity, max 3 attempts, 3 sends/phone/hour (fixed window, fail-open), in-memory |
| BR-A5 | SMS phone format `84xxxxxxxxx`; OTP 4–10 digits; sender ≤ 11 chars; success only if response `status=success` |
| BR-A6 | Customer login identifier = phone or email; account must have an email; principal = email |
| BR-A7 | Admin login: role STAFF or ADMIN, not deleted, `logout=false`, by email or phone |
| BR-A8 | Reset token = sha256Hex(id + email), stored in `account.hash`, cleared after reset; no expiry |
| BR-A9 | Reset: new ≠ current (checked before blank check), not blank, new == confirm |
| BR-A10 | STAFF / admin-settings change password requires old password; ADMIN changing another account and customer `/SA/SA002/change-password` do **not** |
| BR-A11 | Role routing: `/SA/**` ROLE_USER, `/AM/**` ADMIN/STAFF, `/AM/system/**` ADMIN, `/HO/**` MANAGE; controllers additionally require `adminSession` with STAFF/ADMIN |
| BR-A12 | Session timeout 15 days (profiles), remember-me 15 days |
| BR-A13 | Startup seeds a default ADMIN when not found |

## 5. Catalog, cart & misc

| ID | Rule |
|---|---|
| BR-C1 | Storefront hides deleted products; admin sees all |
| BR-C2 | Category filter: SALE / BLACK FRIDAY ⇒ discount > 0; ALL PRODUCTS ⇒ no filter; else `categories LIKE` |
| BR-C3 | Product listing always sorted `updated desc` (Pageable sort ignored); category pages size 12 |
| BR-C4 | "Accessory" product = exactly one variation with blank color, size and type |
| BR-C5 | Related products: up to 4 `relatedProductN` codes, non-deleted |
| BR-C6 | Cart add: replace items with same variationId as the **first** new item, then append; DB cart wins over session on `/cart-detail`; login merge on `firstLogin` |
| BR-C7 | Product validation: price 0..999,999,999, discount 0..99 (discount message wins), ≥1 category, main image on create, code unique on create among non-deleted |
| BR-C8 | Shipping-fee setting 0..100,000 |
| BR-C9 | Variation image update applies to all variations with same pancakeProductId and color |
| BR-C10 | Image compression: only JPEG/PNG > 512,000 B, not already marked; long edge ≥ 1200 px; ≤ 40 MP decode; skip if < 10% saving; backup never overwrites; batch needs free space ≥ needed + 200 MB |
| BR-C11 | Size advice fallback (weight only): female 40–47 S, 48–55 M, 56–62 L, 63–70 XL; male 45–55 S, 56–65 M, 66–75 L, 76–85 XL, 86–95 XXL; < 45 S, > 95 XXL, else M |
| BR-C12 | Master location import only when table empty, batches of 500 |

## 6. Time handling

- No global `TimeZone.setDefault`; JVM default zone used by `LocalDate.now()` (Job01 voucher window), `new Date()` (most timestamps), `DateConst.UTC` (actually system default).
- VNPay create/expire dates formatted in GMT+7; Excel export adds +7 h to `created`.
- `DateUtil` contains JST and Asia/Ho_Chi_Minh formatters.
- Payment-log filters: from inclusive, to + 1 day exclusive (native); QueryDSL variant uses strict `after(from)`.
