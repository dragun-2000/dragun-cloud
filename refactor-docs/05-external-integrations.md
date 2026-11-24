# 05 — External Integrations

Paths relative to `src/main/java/vn/co/cake/`. Secret values are never reproduced here.

| # | System | Client | Direction | Triggered by |
|---|---|---|---|---|
| 1 | Pancake POS | `service/external/PancakePosService` via `pancakeRestTemplate` | Out + inbound webhook | Payment success (after commit, async), Job002/003/01, AM006, CartController order-history, customer cancel, `/api/v2/pancake/*` |
| 2 | VietQR | `payment/vietqr/VietQrApiClient` | Out (QR generate, token) + inbound `/vqr/**` | Checkout, partner callbacks |
| 3 | VNPay | `vnpay/client/VnpayApiClient`, `vnpay/support/VnpaySigner` | Out (pay URL, QueryDR) + inbound `/vnpay/ipn`, `/vnpay/return` | Checkout, IPN, VnpayExpireJob |
| 4 | Brevo (email) | `helper/EmailService` (own Apache RestTemplate, 10s/60s) | Out | Forgot password, order confirmation (async), admin fulfillment alert, Pancake failure dump |
| 5 | TingTing (SMS) | `helper/SmsService` — `new RestTemplate()` **no timeouts** | Out | SA001 register OTP |
| 6 | OpenAI | `ai/service/openai/OpenAIServiceImpl` via `openaiRestTemplate` | Out | `/api/size-advice/**` |
| 7 | Facebook Conversions | `service/impl/PixelConversionServiceImpl` (facebook-java-business-sdk) | Out | UNKNOWN callers — Phase 3 |
| 8 | Local disk "storage" | `service/aws/impl/S3ServiceImpl` (`Files.copy`) | Write | AM003 product image upload (10 copy-pasted blocks), ProductExtendController |
| 9 | Image compression | `service/image/*` | Local FS read/write/backup | Upload hook, admin batch |

## 1. Pancake POS
- Base URL from `pancake.pos.api.url`; **shop id, token, warehouse id come from DB table `pancake_properties`** (`getDefault()`, :889-891). The `pancake.pos.api.token/shop-id` properties are only read by the inactive diagnostic runner.
- Auth: `api_key` query parameter.
- Calls: POST `/shops/{id}/products` (caller commented out), POST `/shops/{id}/orders`, PUT `/shops/{id}/orders/{oid}` (cancel), GET `/products/variations`, GET `/orders/{oid}`, GET `/orders?search=phone`.
- No in-process retry; Job002 retries `SYNC_FAIL`. Idempotency: skip if `pancakeOrderId` set.
- Failure path: writes `log/pancake-curl-*.txt` (with token) and emails chunks to a hardcoded address with `Thread.sleep` (R-04).
- Inbound `/api/v2/pancake/webhook`: payload saved; no signature check. Stock update (`updateStock`) overwrites `remainQuantity`, swallows exceptions; builds a `WebhookHistory` that is apparently never saved (UNKNOWN).

## 2. VietQR
- Outbound token cached under `synchronized`. Credentials `vietqr.client.*`; partner inbound credentials + JWT secret `vietqr.partner.*`.
- Inbound transaction-sync → `VietQrWebhookService.confirmPayment` (REQUIRES_NEW): amount match, idempotency by checkout status + transactionId log row (no row lock).
- Sandbox simulate endpoint commented out in `PaymentCheckoutController:125`; service method `runSandboxPaymentConfirm` still exists (reachability UNKNOWN).

## 3. VNPay
- Pay URL built and signed (HMAC); IPN verified then processed under `PESSIMISTIC_WRITE` on `vnpay_payment`; FINAL = PAID, PAID_ISSUE, FAILED (EXPIRED intentionally non-final).
- `VnpayExpireJob` → QueryDR per txnRef; give up after `vnpay.query-dr-give-up-minutes`.
- Hash secret logged at INFO (R-02).

## 4. Email (Brevo)
- Only transport is Brevo HTTP API. No JavaMail/SES code despite `spring-boot-starter-mail`, `javax.mail`, `aws-java-sdk-ses` dependencies.
- Templates: `order_confirmation_template.html`, `sa-forget_password_template.html`. Store URL normalization duplicated in `EmailService` and `OrderConfirmationMailService`; "debase.vn" hardcoded in both.
- Admin alert (`PaymentFulfillmentAlertScheduler`) sent **synchronously in afterCommit**; subject always `[VIETQR]` even for VNPay.

## 5. SMS / OTP
- TingTing API; config keys still `speedsms.otp.*`. OTP generated with `java.util.Random`, stored in in-memory maps, plaintext logged (R-03).

## 6. OpenAI
- Model default `gpt-4o-mini`; `openai.timeout` property unused (timeouts hardcoded in RestTemplate config). `callGPT` / `callGPTWithHistory` near-duplicates. `SizeAdviceServiceImpl` falls back to a default size on any exception.

## 7. File processing
- Upload dir default `c:/data/external-images/` (property `image.compression.upload-dir`); served from hardcoded `file:/C:/data/external-images/` (R-14). docker-compose mounts `/var/www/html/external-images` — path alignment between container and code is UNKNOWN (likely set via mounted `config/` override).
- Backups `c:/data/external-images-backup/product-<id>/`; restore + delete-backup; free-space check.
- Excel export: Apache POI in `AccountServiceImpl` (AM004 export). `helper/CSVHelper` is a content-type check only.

## 8. Database
- PostgreSQL via Hikari; `ddl-auto=update`. Schema files in `init/` are applied only on first container init. Tables `vnpay_payment`, `vnpay_settings` exist only in migrations. `users`, `categories` tables have no entity (UNVERIFIED).

## 9. Not integrated (documentation drift)
- **Redis / Spring Session**: 0 references in pom or source. Sessions = servlet default + in-memory `ContextRepository`.
- **MinIO**: dependency present, 0 code references; config keys commented.
- **AWS S3 / SES / Secrets Manager**: dependencies present, 0 `com.amazonaws` code; `aws.*` keys unused. Jenkinsfile carries placeholder AWS env values.
- `PROJECT_DOCUMENTATION.md` claims all of the above — stale.

## 10. Phase-5 refactor candidates surfaced so far (NOT approved, for planning only)

| Area | Candidate | Risk |
|---|---|---|
| Payment | VietQR `confirmPayment` vs VNPay `applyPaid` near-duplicate flow; 3 copies of expiry; 2 copies of draft-order creation; `OrderServiceImpl.create` vs `createWithOrderCode`; 4 grand-total computations; 2 VND amount comparators | CRITICAL (business/tx) |
| Infra | 3 after-commit scheduler classes with same pattern | HIGH |
| Utilities | Duplicate store-URL normalization, phone normalization ×2, truncate ×4, ad-hoc `new ObjectMapper()` ×8, `BigDecimalUtil.formatMoney` overloads, `common/` vs `constants/` | LOW–MEDIUM |
| Web | 10 copy-pasted upload blocks in `AM003Controller`; `OpenAIServiceImpl` duplicate request/catch ladder | MEDIUM |
| DTOs | VietQR/VNPay pilot DTO pairs; `DistrictDto`≈`WardDto`; external DTO copies of embeddables; `dto.OrderItem` vs `entity.OrderItem` name clash | LOW–MEDIUM |
| Dead-code candidates (need runtime/external evidence) | `AM005Controller` (empty), `JwtUtil` (no callers), `CustomCartRepository(+Impl)` (empty), `WebhookHistoryRepository`, ~11 unreferenced repository methods, `SA001/SA001.html`, unused pom deps | LOW–MEDIUM; external endpoints UNKNOWN |
