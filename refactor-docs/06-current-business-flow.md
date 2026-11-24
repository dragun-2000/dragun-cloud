# 06 — Current Business Flows

Baseline `a0515d8`. Paths relative to `src/main/java/vn/co/cake/`. Describes **what the code does today**, not what it should do. `UNUSUAL` items are cross-referenced to `07`/`08`/`09` as `U-nn`. `UNKNOWN` = not verified (static reading only; nothing executed).

Verification: produced by 4 parallel read-only scouts; the highest-impact claims were re-checked directly in source (marked ✔). One scout claim was **refuted** (committed OpenAI key — the value is empty).

---

## F1. COD checkout — `POST /api/payment/create` (paymentMethod=COD)

`PaymentCheckoutController:61-103` → `CodCheckoutService:96-158` (`@Transactional`, REQUIRED, **no rollbackFor**).

**Input:** `OrderDetailRequest` {paymentMethod, voucher, fullName, email, phone, note, address, province, district, ward}. `"TRANSFER"` is mapped to `VIETQR` (:161-166).

**Preconditions (controller):** not logged in → 403 · cart = session `cartForm`, else DB cart; empty → 400 · COD grand total (with voucher % applied, FLOOR) must be ≤ 1,500,000 → else 400 · unknown method → 400.

**Sequence:**
1. `OrderServiceImpl.create` → `persistNewOrder`: save Order(status `NEW`, totalAmount = subtotal − **0**, shippingFee = SHIPPING_FEE voucher fee if subtotal < 2,000,000) ✔; per item stock pre-check `remain − softHeld < qty` → throw (and removes the item from the caller's list, U-03).
2. `InventoryReservationService.reserve(order, now+10m)` — `PESSIMISTIC_WRITE` on variations ordered by id; deduct `remainQuantity`/`actualRemainQuantity`; HELD rows `stockDeducted=true`; refresh product stock.
3. `confirm` HELD→CONFIRMED.
4. Delete DB cart, empty session cart.
5. After commit: Pancake sync (F7) + confirmation mail (F11).
6. Log SUCCESS `CHECKOUT_COD`.

**Output:** 200 {resultType: ORDER_PLACED, message} · `CommonServletException` → 400 · other → 500.

**UNUSUAL:** U-01 checked-exception commit (order row with no items may persist on sold-out) · U-02 voucher never reduces `Order.totalAmount`.

## F2. VietQR checkout start — same endpoint, paymentMethod=VIETQR

Controller: `assertCanUseVietQr` (pilot gate, F10) → 400 PILOT_MESSAGE. No amount cap.
`VietQrCheckoutService.startCheckout:71-138` (`@Transactional`, no rollbackFor):
1. `vietqr.enabled` else throw · cart non-empty.
2. Code = `yyMMdd` + zero-padded daily seq from `order_sequence` (read-then-increment, no lock — U-20).
3. grandTotal = subtotal + shipping (no voucher).
4. content = `"DH"+code`, alnum-only, ≤ 23 chars (not upper-cased when truncated).
5. expiresAt = now + max(`vietqr.checkout.expire-minutes`, 1) (default 5).
6. `createWithOrderCode` → status `AWAITING_PAYMENT`, prepaid 0 → `reserve(order, expiresAt)` (HELD).
7. Save `CheckoutPending` PENDING (requestJson snapshot).
8. **External** `VietQrApiClient.generateCustomerQr` inside the tx while variation locks are held (token cached under `synchronized`, refresh at expires_in−30s).
9. Save qrLink/qrCode; INFO log `CHECKOUT_VIETQR_START`.

**Output:** {resultType: QR_PAYMENT, orderId, qrCode, qrLink, amount, content, expiresAt(ms)}.
**UNUSUAL:** QR/token HTTP error throws checked exception → tx **commits** draft order + stock hold + PENDING row without QR, client gets 400 (U-01); `IllegalStateException` rolls back → 500.

## F3. VietQR status polling — `GET /api/payment/status/{id}`, `/checkout/{id}`

`getCheckoutDetails` (REQUIRES_NEW): no login → 403.
1. No pending row → order `PAYMENT_RECEIVED_UNFULFILLABLE` ⇒ PAID_ISSUE; order not in {AWAITING_PAYMENT, CANCELLED} ⇒ PAID (amount = total+shipping); else 400 "Không tìm thấy".
2. Pending of another account → 400.
3. Sync PENDING→PAID if order already progressed.
4. `resolveCheckoutStatus` **writes on a GET**: PENDING and expired ⇒ PAID (if order progressed) or EXPIRED + release stock + AWAITING_PAYMENT→CANCELLED ("(5 phút)" hardcoded) (U-11).

`getCheckoutStatus` has no callers; `runSandboxPaymentConfirm` reachable only from a commented-out endpoint.

## F4. VietQR inbound — partner token & transaction-sync

- `POST /vqr/api/token_generate`: Basic auth vs `vietqr.partner.username/password` → {access_token (HS512 JWT, sub=username), token_type Bearer, expires_in 300}. Bad creds → **400** (not 401) + FAILED log. Malformed base64 not caught → likely 500 (UNKNOWN).
- `POST /vqr/bank/api/transaction-sync`: bad Bearer → 401 {status FAILED, INVALID_TOKEN}; OK → 200 {status SUCCESS}; `CommonServletException` → 400; other → 500.
  `VietQrWebhookService.confirmPayment` (REQUIRES_NEW, rollbackFor=Exception):
  1. Resolve order id from payload.orderId, else content minus `DH`.
  2. Pending PAID/PAID_ISSUE → return OK (idempotent).
  3. transactionId already has SUCCESS log: PENDING ⇒ reprocess; else DUPLICATE_IGNORED.
  4. Reject: no pending · transType ≠ "C" · amount mismatch (HALF_UP, 0 dp) · no draft order. (FAILED logs written before these throws are rolled back — U-12.)
  5. Upsert Payment(SUCCESS, VIETQR), set prepaid.
  6. `ensureConfirmedForPaidOrder` (REQUIRES_NEW, commits independently; re-reserves if hold lost).
  7. Order→`PENDING_SYNC`, pending→PAID, delete cart, after-commit Pancake sync + mail, 2 SUCCESS logs.
  8. Any exception in 6–7 → `markPaidButUnfulfillable`: Order→`PAYMENT_RECEIVED_UNFULFILLABLE`, pending→PAID_ISSUE, delete cart, after-commit admin alert email.
  **UNUSUAL:** EXPIRED pending not rejected → late payment revives a CANCELLED order (U-13); no row lock (U-21).

## F5. VNPay checkout — `POST /api/vnpay/create`, `GET /api/vnpay/status/{txnRef}`

Controller: no login 403 · empty cart 400 · `CommonServletException` 400 · other 500 (generic). Client IP = first of forwarded chain, default 127.0.0.1, ≤ 45 chars.
`VnpayCheckoutService.startCheckout` (rollbackFor=Exception):
1. `vnpay.enabled` ∧ configured (tmnCode, hashSecret, payUrl, returnUrl) ∧ `vnpay_settings.enabled` · pilot gate · cart non-empty.
2. expiresAt = now + `vnpay.expire-minutes` (15); reservation expiry = expiresAt + grace (5) + 60 min.
3. Code = txnRef; order `AWAITING_PAYMENT`; `reserve`.
4. amount = total+shipping; ≤ 0 → throw.
5. Save `VnpayPayment` PENDING (orderInfo accent-stripped; createDate GMT+7).
6. `buildPaymentUrl`: sorted, URL-encoded params, HMAC-SHA512; `vnp_Amount` = VND×100.
7. INFO log — **includes hash secret** (U-30 ✔).
**Output:** {txnRef, paymentUrl, expiresAt}. Cart **not** cleared here.
Status GET: same account only, else 404 → {txnRef, status, amount, responseCode, message}.

## F6. VNPay IPN / return / expiry

- `GET /vnpay/ipn` → always HTTP 200 JSON {RspCode, Message}; any exception → 99. `VnpayIpnService.handle` (rollbackFor=Exception):
  bad signature → 97 · row (`PESSIMISTIC_WRITE`) missing or tmnCode mismatch → 01 · amount mismatch → 04 · status ∈ {PAID, PAID_ISSUE, FAILED} → 02 (EXPIRED is **not** final) · copy vnp_* fields · `00/00` or any `07` → `applyPaid` · else if PENDING → `applyUnpaid(FAILED)` · → 00.
  `applyPaid` (joins tx): upsert Payment, set prepaid, **delete cart before stock confirmation**, `ensureConfirmedForPaidOrder`; error or code 07 → PAID_ISSUE + UNFULFILLABLE + alert; else PAID + `PENDING_SYNC` + after-commit Pancake + mail.
  `applyUnpaid`: release stock; AWAITING_PAYMENT → CANCELLED.
- `GET /vnpay/return` → view `vnpay-return`, no DB writes; model {isLogin, validSignature, success, txnRef, amount/100, message}.
- `VnpayExpireJob` (`30 * * * * ?`, requires `isConfigured()`, not `enabled`): PENDING with expiresAt ≤ now−grace → `reconcile` (REQUIRES_NEW, row lock): QueryDR (exceptions swallowed) → paid/07 (amount mismatch ⇒ log, stays PENDING forever — U-14) / definitely unpaid ⇒ EXPIRED / giveUp (now ≥ expiresAt + give-up minutes) ⇒ EXPIRED (TIMEOUT).

## F7. Pancake order creation (after payment)

`PancakeSyncScheduler.scheduleSyncAfterCommit` (from COD, VietQR webhook, VNPay applyPaid): no tx ⇒ immediate; else afterCommit → `findById` → `@Async syncOrderToPancakeAsync` → `createOrder`:
1. `findOrderWithFullItems` (native, no deleted filter, one row per item — UNKNOWN collapse behavior).
2. Already has `pancakeOrderId` ⇒ status NEW, countError 0, return true (**no `consume`**, U-15).
3. Config from DB `pancake_properties` + warehouse row; missing ⇒ SYNC_FAIL "Missing Pancake config".
4. Build `MainOrderRequest` **outside try** (NPE escapes, order not marked SYNC_FAIL — U-16). Mapping: is_free_shipping=true, note "WEB + {fee} tiền phí ship…", custom_id = order.code, discount = Σ line discount, prepaid only if > 0, items weight 200, retail_price = item.price ∨ variation.retailPrice ∨ 0.
5. POST `/shops/{shop}/orders?api_key=` (Connection: close).
6. 201 + body ⇒ `updateOrderSyncSuccess`: parse id/status/tracking/partner; if no status name, list-by-phone enrich; status NEW, countError 0; `consume` (CONFIRMED→CONSUMED).
7. Otherwise ⇒ `updateOrderSyncFailure`: SYNC_FAIL, countError+1, messageError (≤255) + **curl file with token + chunked email to hardcoded address** (U-31).
Private `@Transactional` on success/failure methods is ineffective (each save commits on its own).

## F8. Pancake background sync

- **Job002** (`0 */30 * * * ?`): `status='SYNC_FAIL' AND count_error < 25 AND deleted=false` → `createOrder` synchronously per order; exceptions logged per order; no tx. ≥ 25 failures ⇒ never auto-retried.
- **Job003** (`0 0 * * * ?`, batch 200): native query `deleted=false AND status NOT IN ('AWAITING_PAYMENT','CANCELLED') AND phone non-blank AND (status name blank OR in hourly names OR code IN (0,1,2,8,9)) ORDER BY pancake_synced_at NULLS FIRST LIMIT 200` → group by phone → detail GET or list-by-phone match → `PancakeOrderSyncWriter.applySuccessfulSync` (REQUIRES_NEW per order) writes mirror columns only. **Never changes `Order.status`**; may set `pancakeOrderId` on a SYNC_FAIL order (then Job002 flips it NEW without consume — U-15).
- **Job01 05:00** & `POST /api/v2/pancake/sync-products` (unauthenticated): paged GET `/products/variations` (page_number from 0, loop while page size == 30; size not sent) → `ProductServiceImpl.syncProductPancake` (no tx): upsert products by productPancakeId (incl. deleted), variations by variationId; stock = Σ remain_quantity; absolute overwrite; removed Pancake items never deleted; `Collectors.toMap` throws on DB duplicates.
- **Webhook** `POST /api/v2/pancake/webhook` (no auth, always 200): parse `UpdateStockResponse` (unknown field ⇒ parse fails silently) → variation.remainQuantity = remain_quantity (absolute, ignores local HELD) → product stock = Σ siblings; all exceptions swallowed; `WebhookHistory` never persisted.

## F9. Customer order history & cancel

- `GET /order-history`: not logged in / no account ⇒ view `login`. `findVisibleOrderHistoryByPhone(phone)` — **by phone, not account** (U-17): `deleted=false AND status<>'AWAITING_PAYMENT' AND (status<>'CANCELLED' OR pancakeOrderId IS NOT NULL)` → synchronous Pancake refresh for all those orders (REQUIRES_NEW per order) → re-query → map to `OrderPancakeResponse` (paid = 0 for COD; online = prepaid ∨ total capped at total; cancelMode NONE/SELF/CONTACT) → view `order-status`. Any exception ⇒ `login`.
- `POST /api/orders/{code}/cancel` (`CustomerOrderCancelService.cancel`, no tx): no session ⇒ 403; must be own, not deleted, status NEW (CANCELLED ⇒ ALREADY_CANCELLED; else 400), must have pancakeOrderId; live GET Pancake: canceled ⇒ mark local CANCELLED; not "new" ⇒ mirror + CONTACT_ADMIN; else PUT `{"status":6}` → local CANCELLED (messageError fixed text, mirror code 6). **No local stock release** (relies on Pancake webhook).

## F10. Payment pilot gating

`GET /api/payment/{vietqr|vnpay}-access` → {visibleToAll, pilotMode, unlocked, canUse*, pilotMessage}. `POST …/unlock {password}`: no login 403; visibleToAll ⇒ unlock without password; else BCrypt match; wrong ⇒ 400. Unlock stored per-session. Defaults: VietQR visibleToAll=true; VNPay enabled=true, visibleToAll=false — **defaults saved inside a readOnly tx** (persistence UNKNOWN). `VnpayModelAdvice` (CartController only): exception ⇒ vnpayEnabled=true, access locked.

## F11. Order confirmation mail

After commit → `@Async sendForOrderAsync` (readOnly tx): flag `order.confirmation.email.enabled` (default true) → reload order with items → email regex check → Thymeleaf HTML + text, subject prefix + code, link `store-url + "/order-history"` (default https://debase.vn) → Brevo. Exceptions logged; no retry; independent of Pancake result.

## F12. Admin order screens

- AM002 list: `deleted=false` + optional code/email contains, status, method, created range (to+1 day), code desc. Detail `GET /AM/AM002-01?code=` (`findFirstByCode`, no deleted filter; unknown code ⇒ `new OrderDetailResponse(null)` — UNKNOWN).
- AM006 list: `status IN (SYNC_FAIL, PAYMENT_RECEIVED_UNFULFILLABLE)`. `POST retry-sync`: no admin 403; not found 404; other status 409; UNFULFILLABLE ⇒ `ensureConfirmedForPaidOrder(now+10m)` (fail ⇒ 409) → PENDING_SYNC → `createOrder` sync (no retry cap) → success log **always METHOD_VIETQR** → 200 / 500. `POST delete`: soft-delete **any** order by code, no status check, no Pancake cancel, no stock release.

## F13. Accounts & authentication

- **Register with OTP** (`/SA/SA001/register/send-otp|verify-otp|resend-otp`, permitAll, CSRF-exempt): send-otp → rate limit 3/phone/hour (in-memory, fail-open) → 6-digit `java.util.Random` OTP → TingTing SMS → store whole request **incl. plaintext password** in memory 5 min → 200 {expiresIn: 300 hardcoded}. **OTP logged at INFO** ✔. No phone/duplicate/password validation before SMS. verify-otp → max 3 attempts → `addAccount` → 201. Third wrong attempt shows "session expired" (unreachable branches).
- **Legacy `POST /SA/SA001/register`** (permitAll, CSRF-exempt) ✔: calls `addAccount` directly **without OTP**; with `accountId` it **updates any account by id** (profile save path used by `signup.js`) — U-32.
- `addAccount` rules: phone regex `^(0|84|\+84)(3[2-9]|5[689]|7[06-9]|8[1-689]|9[0-46-9])[0-9]{7}$`, normalized 10 chars; password == confirm; phone unique among ROLE_USER; height 120–250, weight 30–150 (nullable); BCrypt; ROLE_USER, VALID; address composed with ward/district/province path. No password strength, no email check.
- **Login** (Spring form login): customer by phone then email, **requires non-blank email** (U-18); principal username = email; success handler writes `userSession` (incl. password hash) **before** URL/role validation; firstLogin=true; redirect `/`. Admin: STAFF/ADMIN by email or phone; `adminSession`; redirect AM002. Failure ⇒ 302 to login `?error`. Remember-me key hardcoded, 15 days; session timeout 15d in profiles. Context backup in in-memory map (not Redis).
- **Forgot password**: hash = sha256Hex(id+email) — deterministic, no expiry, no rate limit (U-33). Customer: email via Brevo, result ignored, always 200. Admin: **email send commented out** — link never delivered.
- **Change password** `POST /SA/SA002/change-password` (ROLE_USER + CSRF) → `changePasswordAdmin` ✔: **no old-password check, no ownership check** — any customer can set any account's password by id (U-34). `POST /AM/AM001/change-password` (permitAll path): unauthenticated ⇒ NPE ⇒ error page; STAFF/settings ⇒ own account with old-password check; ADMIN ⇒ any account, no old-password check.
- **Admin accounts** AM004/AM007: list excludes self (`mailAddress <> me` also drops NULL mails); Excel export (9 columns, created+7h); register creates ROLE_USER; update sets phone unvalidated; delete soft-deletes list, swallows errors, always 200. Several handlers `sendRedirect` without `return` (U-22).
- **Startup**: `LoadDatabase` seeds admin with hardcoded credentials when not found (re-creates if the seeded one is soft-deleted).

## F14. Catalog, cart, pricing

- **Selling unit price** (`OrderPricingUtil.resolveSellingUnitPrice`): product.finalPrice > 0 ∨ product.price > 0 ∨ variation.retailPrice ∨ 0. Subtotal: per line `unit×qty` → `longValue()` (truncation), skips qty ≤ 0.
- `GET /`: product search (non-admin ⇒ deleted=false; category SALE/BLACK FRIDAY ⇒ discount>0; ALL PRODUCTS ⇒ none; else LIKE), always `updated desc`; second "NEW IN" query; **`Cache-Control: public, max-age=86400`** on a page with per-user state ✔ (U-23).
- `GET /products?id=` detail; missing/deleted ⇒ `CommonServletException` ⇒ error page (HTTP 200).
- **Cart**: guest cart in session `cartForm`; DB Cart/CartItem per account. `/add-to-cart` replaces items with the first new item's variationId then appends; logged-in ⇒ `cartService.create` (checked exception ⇒ empty Cart row kept, U-01). No stock/quantity/price validation in the cart. `/update-to-cart` updates session only (lost on next `/cart-detail` if DB cart non-empty). Login merge in `HomeController.getCartFormDefault` uses `account.firstLogin`.
- `GET /payment/voucher?code=` returns discountPercent for **any** non-deleted voucher code incl. internal `DISCOUNT_PRICE`/`SHIPPING_FEE`; no date window check.
- **Discounts**: AM008 per-product discount (0<d<100) writes discount/discountPrice(amount)/finalPrice. AM003 settings-discount upserts `DISCOUNT_PRICE` voucher only (apply call commented out). **Job01 every minute**: outside the voucher date window ⇒ reset **all** products' discounts (also wipes AM008 discounts); null discount ⇒ NPE aborts run.
- **Admin products AM003**: validation price 0..999,999,999, discount 0..99, ≥1 category, main image required; code unique on create only; soft delete/restore (`void`, errors swallowed). `PUT /variations/{id}` is **outside `/AM`** ⇒ no security chain (U-24) and sets the image on all variations of the same product+color.
- **Images**: `S3ServiceImpl` writes local disk `millis_safeName`, no type validation, multipart limit 100MB; async compression (JPEG/PNG > 512 KB, marker-based skip, long edge ≥ 1200, quality 0.85→0.70, skip if < 10% gain).

## F15. Location, AI, external CRUD

- `/provinces` (deleted=false, by name), `/districts/{code}`, `/wards/{code}` (no deleted filter). `GET /setup/location` imports master data from classpath JSON when tables are empty — **write behind an unauthenticated GET**.
- **AI size advice** `POST /api/size-advice`: validates productId/gender/height/weight > 0; saves height/weight if logged in; OpenAI (gpt-4o-mini, T 0.7, 500 tokens) → parse JSON → size-availability fix-ups; **any exception ⇒ weight-only fallback table** (female 71–95 kg ⇒ "M"). `/chat`: regex context parse; "female" contains "male" ⇒ male (U-25). Prompts logged at INFO.
- **External CRUD (no auth)**: `/api/extend/products` (PUT upserts, DELETE hard-deletes), `/api/extend/orders` (`/customer/{id}` ignores id, returns hardcoded "John Doe" orders), `/api/extend/shops` (hard delete), `/api/thiyen/products` (PUT appends child collections).
- `PixelConversionServiceImpl`: no callers (dead-code candidate).
