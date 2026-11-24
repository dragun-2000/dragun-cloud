# 11 — Test Coverage Gaps & Characterization-Test Proposal

Baseline `a0515d8`. Coverage was **not measured** (no JaCoCo); this is a class-level map from test imports and the classes each test instantiates. Flow IDs → `06`, rules → `07`, unusual behaviors → `09`.

## 1. What the existing 53 tests cover

| Area | Covered classes | Depth |
|---|---|---|
| Image compression | `ImageCompressor`, `ProductImageCompressionService` (with temp dirs, mocked repositories), `image-compression.html` render | Good (21 tests) |
| Pancake status mapping | `PancakeFulfillmentStatus`, `PancakeOrderStatus` | Good for mapping/cancelMode (8 tests) |
| VNPay | `VnpaySigner`, `VnpayFormats`, `VnpayIpnService` (repository + result service mocked) | Good for signature/amount/response codes (21 tests); `applyPaid`/`applyUnpaid` effects not exercised |
| Local file "S3" | `S3ServiceImpl` file-name sanitizing | Minimal (2 tests) |

Effective coverage of the ~308 production classes: roughly **12 classes** exercised (~4%).

## 2. Critical flows with NO tests

| Flow | Key classes | Risk if refactored blind |
|---|---|---|
| F1 COD checkout | `CodCheckoutService`, `OrderServiceImpl`, `PaymentOrderTotalCalculator` | CRITICAL |
| F2–F4 VietQR | `VietQrCheckoutService`, `VietQrWebhookService`, `VietQrPartnerTokenService`, `VietQrContentNormalizer`, `VietQrApiClient` | CRITICAL |
| F5–F6 VNPay (non-IPN) | `VnpayCheckoutService`, `VnpayPaymentResultService`, `VnpayExpireService`, `VnpayApiClient` | CRITICAL |
| Stock reservation | `InventoryReservationService` (reserve/confirm/consume/release/ensureConfirmed) | CRITICAL |
| F7–F8 Pancake sync | `PancakePosService` (create/update/cancel/stock/batch), `PancakeOrderSyncWriter`, `MainOrderRequest` mapping, Jobs 002/003/01 | HIGH |
| F9 Order history / cancel | `CartController#orderHistory`, `CustomerOrderCancelService` | HIGH |
| F11 Mail | `OrderConfirmationMailService`, `EmailService`, after-commit schedulers | MEDIUM |
| F13 Accounts & auth | `AccountServiceImpl`, `OtpVerificationService`, `SmsService`, `PhoneValidator`, security configs/handlers, `UserDetailsServiceImpl` | CRITICAL (security) |
| F14 Catalog / cart / pricing | `OrderPricingUtil`, `Product` price methods, `CartServiceImpl`, `HomeController`, `ProductServiceImpl` (discount reset, Pancake product sync) | HIGH |
| F15 AI / location / external CRUD | `SizeAdviceServiceImpl` (fallback, availability fix-up, chat parsing), `MasterUtil`, Extend services | MEDIUM |
| Exception contract | `GlobalExceptionHandler` (API vs page detection, HTTP 200 error page) | HIGH |
| Repositories | QueryDSL custom repos, native queries in `OrderRepository`, `CustomPaymentTransactionLogRepositoryImpl` SQL | HIGH (needs DB) |
| Utilities | `DateUtil`, `BigDecimalUtil`, `PhoneValidator`, truncate/normalize helpers | LOW–MEDIUM (cheap to cover) |

## 3. Behaviors that can only be verified with a real DB / runtime (currently UNVERIFIED)

- U-01 partial commit on checked `CommonServletException`; `UnexpectedRollbackException` path in COD.
- U-41 `findOrderWithFullItems` with multi-item orders.
- U-42 pilot defaults saved in readOnly tx.
- Effective `@Async` executor (R-07), entity generator allocation (R-11), QueryDSL `fetchCount` with paging, `findAllByDeletedFalse` null semantics.
- Native SQL in Job003 / AM010 log summary.

> **Status update 2026-11-24:** Tier A (CT-A1…CT-A8) is **COMPLETED** — 17 files, 244 runs, all green; full suite 297 run / BF-01 only. Class-level coverage rose from ~12 to ~30 production classes. Tier B and Tier C remain proposals. Details: `10-baseline-results.md` §6; new behaviors pinned: `09-side-effects.md` U-45…U-60.

## 4. Proposed characterization tests (Tier A written; B/C need approval)

Principles: assert **current** behavior (including the U-items), no production code change, no new dependencies unless approved, no external calls, no production DB. Test style copies the existing suite (JUnit 5 + Mockito, constructor-built services, `@TempDir`).

### Tier A — pure unit, no new dependencies (recommended first)

| ID | Target | What to pin | Est. tests |
|---|---|---|---|
| CT-A1 | `OrderPricingUtil`, `Product` create/update/discount math, `OrderServiceImpl.calculateItemsSubtotal/calculateGrandTotal` (repos mocked) | BR-P1, P2 (truncation), P3, P4 (voucher ignored — U-02), P7 | ~15 |
| CT-A2 | `PaymentOrderTotalCalculator` | BR-P6 COD limit incl. FLOOR | ~5 |
| CT-A3 | `VietQrContentNormalizer`, VietQR amount match, `VnpayFormats` gaps | content rules, HALF_UP match | ~8 |
| CT-A4 | `PhoneValidator`, `OtpVerificationService` (in-memory: expiry, attempts, rate limit fail-open) | BR-A1, A4, unreachable-message behavior | ~12 |
| CT-A5 | `MainOrderRequest` payload mapping | BR-P12 field-by-field | ~6 |
| CT-A6 | `SizeAdviceServiceImpl` fallback table + availability fix-up + chat context parse | BR-C11, U-25 | ~12 |
| CT-A7 | `GlobalExceptionHandler` (direct invocation with `MockHttpServletRequest`) | 08 §2 table incl. HTTP 200 page, `/api` detection | ~10 |
| CT-A8 | Utilities: `DateUtil`, `BigDecimalUtil`, truncate/normalize helpers slated for de-duplication | exact outputs incl. null/empty | ~20 |

### Tier B — service-level with Mockito (no DB)

| ID | Target | What to pin |
|---|---|---|
| CT-B1 | `InventoryReservationService` | reserve deducts + HELD, confirm/consume/release transitions, re-reserve in `ensureConfirmedForPaidOrder`, sold-out exception |
| CT-B2 | `VietQrWebhookService.confirmPayment` | idempotency branches, transType/amount rejects, paid vs unfulfillable, EXPIRED revival (U-13) |
| CT-B3 | `VnpayPaymentResultService` / `VnpayExpireService` | applyPaid/applyUnpaid, 07 handling, QueryDR mismatch stays PENDING (U-14), give-up |
| CT-B4 | `CodCheckoutService` / `VietQrCheckoutService.startCheckout` | call order, statuses set, after-commit scheduling (tx-sync absent path) |
| CT-B5 | `PancakePosService.createOrder` / `updateStock` / `cancelOrderOnPancake` (RestTemplate mocked; failure-dump email path asserted but not sent) | SYNC_FAIL counting, idempotent branch (U-15), NPE outside try (U-16), silent stock parse failure (U-28) |
| CT-B6 | `CustomerOrderCancelService` | BR-O8 branches |
| CT-B7 | `AccountServiceImpl` | addAccount create/update branches (incl. U-32 update-by-id), reset/change password (U-33, U-34), height/weight bounds |
| CT-B8 | Job004 / Job002 / Job01 discount reset | selection + per-item behavior (U-19, U-38) |

Note: Tier B cannot observe real transaction semantics (rollback/commit); those stay UNVERIFIED until Tier C.

### Tier C — integration with a disposable PostgreSQL (needs separate approval)

Options: (1) local `docker compose up debase-db` with a **throwaway volume/database name**, or (2) Testcontainers (**new test dependency**). Scope: native/QueryDSL repository queries, U-01/U-41/U-42 transaction outcomes, `@WebMvcTest`/MockMvc for security chain matching (`/api/**` unprotected, `/AM/AM001/**` permitAll, CSRF rules) and response contracts. Must never point at a shared or production DB (note `ddl-auto=update`).

## 5. Recommendation

1. Approve **Tier A** now (≈90 tests, zero production change, zero new dependency) — it covers the utilities/DTO/pricing areas most likely to be in the first low-risk refactor batch.
2. Approve **Tier B** for any module that the Phase 5 plan proposes to touch, before touching it.
3. Decide on **Tier C** (docker-compose DB vs Testcontainers) before any refactor in payment/checkout/security/repository code; until then those modules stay out of scope.
4. Optionally approve an ad-hoc JaCoCo run (no pom change) to replace the estimate in §1 with measured numbers.
5. BF-01: owner to decide whether the label `Check Sản Phẩm` is intended; until then it remains an expected baseline failure.
