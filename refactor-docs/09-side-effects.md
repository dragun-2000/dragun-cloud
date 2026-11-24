# 09 — Side Effects & Unusual Behavior Register

Baseline `a0515d8`. Paths relative to `src/main/java/vn/co/cake/`.

## 1. Side-effect inventory

| Side effect | Triggered by | Inside tx / after commit | Sync/async |
|---|---|---|---|
| Stock deduct (HELD) | COD, VietQR start, VNPay start, `ensureConfirmedForPaidOrder` | inside (row locks) | sync |
| Stock release | Job004, VietQR status GET, VNPay unpaid/expiry | inside | sync |
| Stock overwrite (absolute) | Pancake webhook, Job01 05:00 / `sync-products` | no tx | sync |
| Cart delete | COD, VietQR paid/issue, VNPay applyPaid | inside | sync |
| Pancake order create | `PancakeSyncScheduler` (after commit), Job002, AM006 | after commit (async) / none | async / sync |
| Pancake order cancel (PUT status 6) | customer cancel | no tx | sync |
| Pancake GET (detail / list by phone) | Job003, `/order-history` page load, customer cancel, success enrichment | REQUIRES_NEW per write | sync |
| VietQR QR generate / token | VietQR start | **inside tx, locks held** | sync |
| VNPay QueryDR | VnpayExpireJob | **inside tx, row lock held** | sync |
| Order confirmation email (Brevo) | paid / COD | after commit | async |
| Admin fulfillment alert email | paid-but-unfulfillable | after commit, **synchronous in request thread** | sync |
| Forgot-password email | SA002 send-mail (admin send commented out) | inside `AccountServiceImpl` tx | sync |
| Pancake failure email (chunked, `Thread.sleep` 800 ms per part) + `log/pancake-curl-*.txt` (contains `api_key`) | Pancake create failure | no tx | sync in async/job thread |
| SMS OTP (TingTing, no timeouts) | register send/resend OTP | none | sync |
| OpenAI call | size advice | none | sync |
| File write `C:/data/external-images` (or configured) | AM003 uploads, ProductExtend, `PUT /variations/{id}` | none | sync |
| Image compress in place / backup / restore | upload hook, admin batch | none | own executors |
| Payment transaction log rows | all payment flows, AM006 | joins caller tx (rolled back with it) | sync |
| `order_sequence` increment | every checkout | inside | sync |
| Master data import | `GET /setup/location` | `@Transactional` | sync |
| Admin seed | startup | — | sync |
| `account.firstLogin=true` | every customer login | — | sync |
| Global bookmark keys | every unauthenticated request (ContextService) | in-memory | sync |
| Sensitive logs | VNPay hash secret (INFO), OTP + phone (INFO), AI prompts (INFO), phones in warnings | — | — |
| HTTP cache | `GET /` sends `Cache-Control: public, max-age=86400` | — | — |

## 2. Unusual existing behavior register

Each item is **current behavior to preserve during refactoring**. Fixing any of these is a separate, explicitly approved change. Severity = impact of the existing behavior.

| ID | Behavior | Evidence | Severity |
|---|---|---|---|
| U-01 | Checked `CommonServletException` doesn't roll back REQUIRED tx without rollbackFor → partial commits (order w/o items, draft order+hold w/o QR, empty Cart) | `exception/CommonServletException.java:8` ✔; see 08 §1 | HIGH |
| U-02 | Voucher discount never applied to `Order.totalAmount`; COD limit uses voucher-discounted total | `OrderServiceImpl:155,166` ✔ | HIGH (business) |
| U-03 | `persistNewOrder` removes sold-out item from caller's list before throwing | `OrderServiceImpl:187` | LOW |
| U-11 | Status GET endpoints release stock and cancel orders | `VietQrCheckoutService:244-278` | MEDIUM |
| U-12 | FAILED payment logs before throws are rolled back | `VietQrWebhookService:106,118,129` | MEDIUM |
| U-13 | Late VietQR payment / VNPay IPN after EXPIRED revives CANCELLED order | `VietQrWebhookService`, `VnpayIpnService:35` | HIGH |
| U-14 | VNPay QueryDR amount mismatch keeps row PENDING forever (re-queried each minute) | `VnpayExpireService:52-107` | MEDIUM |
| U-15 | Pancake idempotent branch flips to NEW without `consume`; Job003 can set pancakeOrderId on SYNC_FAIL orders | `PancakePosService:202-213` | MEDIUM |
| U-16 | Pancake payload built outside try → NPE escapes, order not marked SYNC_FAIL | `PancakePosService:244-245` | MEDIUM |
| U-17 | Order history matched by phone, not account | `OrderRepository:25-29` | HIGH (privacy) |
| U-18 | Customers without email cannot log in | `UserDetailsServiceImpl:54-57` | MEDIUM |
| U-19 | Job004 selects **all** expired HELD reservations incl. VNPay; cancels VNPay orders with VietQR message | `InventoryReservationRepository:31-33` ✔ | HIGH |
| U-20 | Order code generation read-then-increment without lock | `OrderServiceImpl:276-294` | HIGH |
| U-21 | VietQR webhook takes no row lock; Job004 may overwrite concurrently paid order | `VietQrWebhookService`, `Job004:89-95` | HIGH |
| U-22 | Handlers call `sendRedirect` without `return` (execution continues) | `AM003Controller:141,165,296,472-497`, `AM004Controller:55-57,69-89`, `AM007Controller:47,93` | MEDIUM |
| U-23 | Home page publicly cacheable 24 h despite per-user content | `HomeController:124` ✔ | MEDIUM |
| U-24 | `PUT /variations/{id}` outside `/AM/**` → no security chain; upload before existence check | `AM003Controller:538` | HIGH (security) |
| U-25 | Chat gender parse: "female" ⇒ male | `SizeAdviceServiceImpl:876-948` | LOW |
| U-26 | Customer role-mismatch redirect doubles path | `UserSuccessHandler` | LOW |
| U-27 | `@Transactional` on private methods / self-invocation ineffective in `PancakePosService` | `:163,525,639` | MEDIUM (refactor trap) |
| U-28 | Pancake stock webhook: unknown JSON field ⇒ silent no-op; partial updates; history never saved | `PancakePosService:96-128` | MEDIUM |
| U-29 | Admin alert subject always `[VIETQR]`; AM006 success log always METHOD_VIETQR | `PaymentFulfillmentAlertScheduler:42`, `AM006Controller` | LOW |
| U-30 | VNPay hash secret logged at INFO | `VnpayCheckoutService:121-122` ✔ | CRITICAL (security) |
| U-31 | Pancake failure: token written to file and emailed to hardcoded personal address | `PancakePosService:670-741` ✔ | HIGH (security) |
| U-32 | `POST /SA/SA001/register` (permitAll, CSRF-exempt): creates account without OTP; with `accountId` **edits any account** | `SA001Controller:293-309`, `AccountServiceImpl:141-145` ✔ | CRITICAL (security) |
| U-33 | Reset token deterministic sha256(id+email), no expiry, no rate limit; admin reset email never sent | `AccountServiceImpl:250-312` | HIGH (security) |
| U-34 | `/SA/SA002/change-password` → `changePasswordAdmin`: no old-password, no ownership check — any customer can reset any account (incl. admin) by id | `AccountServiceImpl:368-389` ✔ | CRITICAL (security) |
| U-35 | OTP logged in plaintext; registration password held in memory as plaintext JSON | `SA001Controller:170` ✔, `OtpVerificationService:46-66` | HIGH (security) |
| U-36 | Unauthenticated write endpoints: `/api/extend/**` (hard deletes), `/api/thiyen/**`, `/api/v2/pancake/*`, `GET /setup/location` | see 03 §4 | CRITICAL (security) |
| U-37 | `/api/extend/orders/customer/{id}` ignores id, returns hardcoded "John Doe" orders | `OrderExtendService` | LOW |
| U-38 | Job01 resets **all** product discounts (incl. AM008 manual ones) every minute outside the voucher window; null discount aborts | `Job01:31-49`, `ProductServiceImpl:266-279` | MEDIUM |
| U-39 | `/payment/voucher` exposes internal voucher codes; no date window check | `CartController:190-197` | LOW |
| U-40 | AM006 delete soft-deletes any order without Pancake cancel or stock release | `OrderServiceImpl:235` | MEDIUM |
| U-41 | `findOrderWithFullItems` native join returns one row per item for a single-entity result | `OrderRepository:65-73` | MEDIUM (UNKNOWN runtime) |
| U-42 | Pilot settings defaults saved inside readOnly tx | `VietQrPilotAccessService:35-38`, `VnpaySettingsService:21-25` | LOW (UNKNOWN) |
| U-43 | Global (non-per-user) bookmark keys written on every unauthenticated request | `ContextService:94-111` | MEDIUM |
| U-44 | `/update-to-cart` changes session only; lost when DB cart non-empty | `CartController:162` | LOW |

### 2.1 Pinned by Tier A characterization tests (executable evidence)

| ID | Behavior | Evidence (test) | Severity |
|---|---|---|---|
| U-45 | Pancake order JSON keys for Lombok `is_*` fields serialize as `_free_shipping`, `_bonus_product`, `_discount_percent`, `_wholesale` (the `is_` prefix is lost) — the payload actually sent to Pancake does not contain `is_free_shipping` | `MainOrderRequest.java:15,70`; `MainOrderRequestCharacterizationTest` | HIGH (integration contract — verify against Pancake before any change) |
| U-46 | `warehouse_info` nested fields serialize camelCase; note text prints raw BigDecimal fee while `shipping_fee` truncates | `MainOrderRequest.java:46,127` | LOW |
| U-47 | Product create/update ignore discounts < 1% (`discount.longValue() > 0`), Pancake sync applies them; null discount on create ⇒ NPE; discount amount scale follows price×discount | `Product.java:119,178,234`; `ProductPricingTest` | MEDIUM |
| U-48 | Line discount sum truncated per line with `intValue()` | `OrderPricingUtil.java:100` | LOW |
| U-49 | `resolveUnitDiscount` mixes variation retailPrice with product finalPrice when product price missing; retailPrice 0 accepted | `OrderPricingUtil.java:31,47` | LOW |
| U-50 | SHIPPING_FEE voucher with null fee ⇒ NPE below threshold | `OrderServiceImpl.java:93`, `PaymentOrderTotalCalculator.java:56` | MEDIUM |
| U-51 | COD total: free-shipping threshold checked before voucher discount; voucher % uncapped (150% ⇒ negative) | `PaymentOrderTotalCalculator.java:31,48` | MEDIUM |
| U-52 | VietQR content: null code ⇒ `"DHNULL"`; `Đ/đ` dropped (not mapped to D) | `VietQrContentNormalizer.java:19,29` | LOW |
| U-53 | VNPay `amountMatches` rounds only the expected side; fractional received amounts never match | `VnpayFormats.java:58` | LOW |
| U-54 | OTP: correct code does **not** delete stored data (re-verifiable); null code ⇒ NPE; keyed by raw phone string; first request in window passes even with limit 0 | `OtpVerificationService.java:48,101,117,136` | MEDIUM (security) |
| U-55 | API detection: any URI starting `/api` (e.g. `/apiary`); missing request param on API ⇒ 500 not 400; IAE message exposed; model `url` is a `StringBuffer`; JSON path array indexes shown as `"null"` | `GlobalExceptionHandler.java:41,58,73-75,120,195,215` | LOW–MEDIUM |
| U-56 | Size advice: gender not trimmed (`"female "` ⇒ male table); null gender ⇒ NPE; null height rendered `"nullcm"`; unavailable XL kept when reasons suggest available size; blank backupSize ⇒ null | `SizeAdviceServiceImpl.java:420-442,569,777,786` | LOW |
| U-57 | `DateConst.UTC` is the JVM default zone captured at class load, not UTC | `DateConst.java:38` | MEDIUM (time handling) |
| U-58 | `DateUtil` parsing is lenient (`2026-13-45` ⇒ 2027-02-14; trailing text ignored) | `DateUtil.java:68` | MEDIUM |
| U-59 | Password rule counts a space as the special character; email regex rejects TLDs > 4 letters | `StringConst.java:48-49` | LOW |
| U-60 | `PageUtil.next()/first()` use wrapped page size; `withPage()` returns null; `FunctionUtil.allPropertiesIsNull` counts static fields; `formatMoney` locale-dependent, HALF_EVEN | `PageUtil.java:55-67`, `FunctionUtil.java:35`, `BigDecimalUtil.java:85` | LOW |

## 3. Refuted / corrected scout claims

| Claim | Result |
|---|---|
| "A real-looking `openai.api.key` value is committed in `application.properties:83`" | **REFUTED** — value is empty (classified without printing) |

## 4. Implications for refactoring scope

- **No-touch without characterization tests:** all payment/checkout services, `InventoryReservationService`, `OrderServiceImpl`, `PancakePosService`, schedulers, jobs, security configuration & handlers, `GlobalExceptionHandler`, `AccountServiceImpl` (CRITICAL/HIGH).
- **Behavior traps:** exception types (checked vs unchecked), `rollbackFor`, private/self-invoked `@Transactional`/`@Async`, HTTP 200 error page, `sendRedirect` without return, String status literals, `longValue()` truncation, absolute stock overwrite, entity generator names (with `ddl-auto=update`).
- **Lower-risk candidates** (still need approval + tests): duplicated pure utilities (string truncate, store-URL normalization, phone normalization), DTO duplicates, unused imports/locals, empty classes, logging message cleanup that does not change log content consumers depend on.
- **Security findings U-24, U-30–U-36** are recommended as **separate, prioritized bug-fix tickets** outside this behavior-preserving refactor.
