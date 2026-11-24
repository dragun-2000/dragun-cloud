# 03 — Entry Points

Paths relative to `src/main/java/vn/co/cake/`. "View X" = Thymeleaf view. Auth column: chain from `02-module-dependency.md` §3 — **none** = no security chain applies.

## 1. Process entry

- `Application.main` — Spring Boot; `@EnableScheduling`; `@EnableAsync` on `config/ThreadPoolManager`.
- Startup runners: `config/LoadDatabase` (CommandLineRunner — seeds an admin account if missing, hardcoded credentials); `config/PancakePosDiagnosticRunner` (ApplicationRunner, only if `pancake.pos.diagnostic-on-startup=true`; property commented out → inactive).

## 2. HTTP — admin (`/AM/**`, admin chain)

| Controller | Endpoints |
|---|---|
| AM001 | GET `/AM`, `/AM/AM001`, `/login`, `/forget-password`, `/change_password_view/{accountId}`, `/verify_user`; POST `/send-mail`, `/reset_password`, `/change-password` (all `/AM/AM001/**` public) |
| AM002 | GET `/AM/AM002`, `/AM/AM002-01` (order list/detail) |
| AM003 | GET `/AM/AM003`, `-01`, `-02/{id}`; POST `-01/register`, `-02/update`, `/settings`, `/settings-discount`, `/deleted`, `/restore`; **PUT `/variations/{variationId}` (no `/AM` prefix → no chain)** |
| AM004 / AM007 | Account list/register/update/delete, GET `/AM/AM004/export-excel`; AM007 serves `/AM/AM004-02*`, `/AM/AM004/update`, `/AM/AM004/deleted` |
| AM005 | Empty class, no handlers |
| AM006 | GET `/AM/AM006`; POST `/retry-sync`, `/delete` (JSON) — Pancake re-sync / recreate |
| AM008 | GET `/AM/AM008`; POST `/update-discount` |
| AM009 | GET `/AM/AM009`; POST `/update-status`, `/update-all-status` (province enable) |
| AM010 | GET `/AM/AM010`, `/order-logs`, `/detail/{id}` (payment logs) |
| AM011 | GET `/AM/AM011`; POST `/save`, `/vnpay/save` (pilot settings) |
| system/ImageCompressionController | `/AM/system/image-compression`: GET page, POST `/compress`, `/products/{id}/restore`, `/products/{id}/delete-backup`, GET `/products/{id}/status` (ADMIN) |

## 3. HTTP — customer (`/SA/**`, user chain)

| Controller | Endpoints |
|---|---|
| SA001 | `/SA/SA001/login` (any method), GET `/SA`, `/SA/SA001`, `/register-view`, `/profile`; POST `/register`, `/register/send-otp`, `/verify-otp`, `/resend-otp` |
| SA002 | GET `/forget-password`, `/change_password_view/{accountId}` (→ missing template), `/verify_user`; POST `/send-mail`, `/reset_password`, `/change-password` |

## 4. HTTP — storefront & APIs (**no security chain**)

| Controller | Endpoints | Own auth check |
|---|---|---|
| store/HomeController | GET `/`, `/products/group/{category}`, `/products/search`, `/products`, `/privacy-policy` | n/a (public) |
| store/CartController | GET `/cart-detail`, `/payment/voucher`, `/payment/vietqr`, `/payment-detail`, `/order-history`; POST `/add-to-cart`, `/update-to-cart`, `/cart/sync-session` | session |
| store/CustomerOrderCancelController | POST `/api/orders/{code}/cancel` | session |
| LocationController | GET `/provinces`, `/districts/{provinceCode}`, `/wards/{districtCode}`, `/setup/location` | **none** |
| payment/api/PaymentCheckoutController | POST `/api/payment/create`; GET `/status/{id}`, `/checkout/{id}` | session |
| payment/api/VietQrPilotAccessController | GET `/api/payment/vietqr-access`, POST `/unlock` | session |
| vnpay/api/VnpayCheckoutController | POST `/api/vnpay/create`; GET `/status/{txnRef}` | session |
| vnpay/api/VnpayPilotAccessController | GET `/api/payment/vnpay-access`, POST `/unlock` | session |
| ai/controller/SizeAdviceController | POST `/api/size-advice`, `/save-profile`, `/chat` | session |
| external/OrderExtendController | `/api/extend/orders` POST, GET, GET `/{id}`, PUT `/{id}/status`, GET `/customer/{id}` | **none** |
| external/ProductExtendController | `/api/extend/products` GET, GET `/{id}`, POST `/create`, PUT `/update/{id}`, DELETE `/{id}` | **none** |
| external/ShopExtendController | `/api/extend/shops` CRUD | **none** |
| external/PancakeController | POST `/api/v2/pancake/webhook` (saves payload, no signature), POST `/sync-products` | **none** |
| thiyen/ProductController | `/api/thiyen/products` GET `/list`, GET `/{id}`, POST, PUT, DELETE | **none** |

## 5. HTTP — inbound payment callbacks (permitAll chains)

| Endpoint | Handler | Verification |
|---|---|---|
| GET `/vnpay/ipn` | `VnpayCallbackController` → `VnpayIpnService.handle` | HMAC signature + row lock + final-status idempotency |
| GET `/vnpay/return` | `VnpayCallbackController` → view `vnpay-return` | No DB writes |
| POST `/vqr/api/token_generate` | `VietQrPartnerTokenController` | Partner basic credentials → JWT |
| POST `/vqr/bank/api/transaction-sync` | `VietQrTransactionSyncController` → `VietQrWebhookService.confirmPayment` | Bearer JWT checked in controller (401) |

## 6. Scheduled jobs

| Job | Schedule | Action |
|---|---|---|
| `job/Job01` | `0 * * * * ?` (every minute) | Reset product discount prices per voucher window |
| `job/Job01` | 05:00 daily | Sync product variations/stock from Pancake |
| `job/Job002` | every 30 min | Retry `SYNC_FAIL` orders to Pancake |
| `job/Job003` | hourly | Pull order status from Pancake (`@Transactional` loop) |
| `job/Job004` | every minute | Expire HELD reservations (VietQR wording, but query matches **all** HELD incl. VNPay) → release stock, CANCEL order |
| `vnpay/job/VnpayExpireJob` | every minute at :30 | VNPay QueryDR reconcile / give-up expiry |

## 7. Async / event entry points

- `@Async PancakePosService.syncOrderToPancakeAsync(...)` (two overloads; one self-invokes the other → inner `@Async` bypassed).
- `@Async OrderConfirmationMailService.sendForOrderAsync`.
- `@EventListener` `security/listener/AuthenticationEventListeners` (logging only).
- After-commit `TransactionSynchronization` hooks: `PancakeSyncScheduler`, `OrderConfirmationMailScheduler`, `PaymentFulfillmentAlertScheduler`.
- Image compression executors: `ImageCompressionService` (1 thread, queue 500), `ProductImageCompressionService` (1 thread, queue 10 000) + JVM shutdown hooks.

## 8. Views & static resources

- Templates under `src/main/resources/templates/**` resolved via `common/ScreenPathConst`. Known broken: `SA002/SA002_03`. Unreferenced candidate: `SA001/SA001.html`.
- Static: `/css/**`, `/js/**`, `/images/**` (ignored by security), `/external-images/**` → `file:/C:/data/external-images/` (`config/WebConfig.java:56-57`).
- Mail templates: `templates/template/mail/order_confirmation_template.html`, `sa-forget_password_template.html` (via `mailTemplateEngine`).
