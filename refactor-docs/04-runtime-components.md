# 04 — Runtime-Managed Components

Components whose execution is driven by Spring/JVM rather than by a direct call. **None of these may be classified as dead code from static references alone.** Paths relative to `src/main/java/vn/co/cake/`.

## 1. Configuration beans

| Class | What it registers | Notes |
|---|---|---|
| `config/ThreadPoolManager` | `@EnableAsync`; bean `threadPoolExecutor` (`ThreadPoolExecutorFactoryBean`, props `threadpoll.*` — misspelled prefix) | keepAlive set from property then overwritten with 30 (:25 vs :29). Not a `TaskExecutor` and not named `taskExecutor` → `@Async` likely uses `SimpleAsyncTaskExecutor` (**UNVERIFIED at runtime**) |
| `config/AuditingAutoConfiguration` | `@EnableJpaAuditing` → `BaseEntity` created/updated/creator/updater | |
| `config/PancakeRestTemplateConfig` | `pancakeRestTemplate` (connect 45s / socket 60s / pool wait 10s), `openaiRestTemplate` (30s/60s hardcoded) | |
| `payment/config/VietQrRestTemplateConfig` | VietQR RestTemplate | |
| `config/MailTemplateConfig` | `mailTemplateEngine` | |
| `config/WebConfig` | Resource handlers incl. `/external-images/**` | Hardcoded `C:/data/...` path |
| `@ConfigurationProperties` | `OpenAIConfig`, `ImageCompressionProperties`, `VietQrProperties`, `VnpayProperties` (last three + OpenAI have `@PostConstruct` validation/logging) | |
| `config/LoadDatabase` | CommandLineRunner: seed admin | Runs every startup |
| `config/PancakePosDiagnosticRunner` | `@ConditionalOnProperty` ApplicationRunner | Inactive (property commented) |
| 4 × `@EnableWebSecurity` | Filter chains (see 02 §3) | |
| `handler/GlobalExceptionHandler`, `vnpay/config/VnpayModelAdvice` | `@ControllerAdvice` (the latter adds VNPay model attrs to `CartController` requests) | |
| Spring Boot auto-config | `JavaMailSender` (unused), Hikari, Thymeleaf, Jackson `ObjectMapper` (but 8 classes `new ObjectMapper()`) | |

## 2. Scheduled jobs

See `03-entry-points.md` §6. All run on Spring's default **single-threaded** scheduler (no `TaskScheduler` bean / `spring.task.scheduling.pool.size` found) → a slow job (e.g. Job003 hourly Pancake pull, VNPay QueryDR) delays all others, including the every-minute Job004/Job01. UNVERIFIED at runtime.

## 3. Async execution

| Method | Caller | Behavior |
|---|---|---|
| `PancakePosService.syncOrderToPancakeAsync(Order)` → self-calls `(Long)` overload | `PancakeSyncScheduler` (after commit), AM006 | Inner `@Async` bypassed (self-invocation). `updateOrderSyncSuccess/Failure` are **private `@Transactional`** → no transaction; DB writes through `PancakeOrderSyncWriter.applySuccessfulSync` (`REQUIRES_NEW`) |
| `OrderConfirmationMailService.sendForOrderAsync` | `OrderConfirmationMailScheduler` | `@Async` + `@Transactional(readOnly)`; flag `order.confirmation.email.enabled` |
| Image compression | Upload / admin batch | Dedicated single-thread executors + `Semaphore(max-concurrent)`; daemon threads; shutdown hooks |

## 4. Transactions (key boundaries)

| Component | Boundary |
|---|---|
| `OrderServiceImpl` | Class-level `@Transactional` |
| `CartServiceImpl` | Class `readOnly=true`; `create`, `deletedCartByAccount` read-write |
| `AccountServiceImpl` | Class `REQUIRED, rollbackFor=Throwable` |
| `ProductServiceImpl` | **None** |
| `CustomerOrderCancelService.cancel` | **None** (Pancake call, then local save) |
| `InventoryReservationService` | `ensureConfirmedForPaidOrder`, `reserveAndConfirmInNewTransaction` = `REQUIRES_NEW`; `reserve/confirm/release` `rollbackFor=Exception`; `consume` plain |
| `VietQrWebhookService.confirmPayment` | `REQUIRES_NEW, rollbackFor=Exception` → nested `REQUIRES_NEW` (2 connections per webhook) |
| `VietQrCheckoutService` | `startCheckout` tx; `getCheckoutStatus` tx **with writes**; `getCheckoutDetails` `REQUIRES_NEW`; `runSandboxPaymentConfirm` none |
| `VnpayCheckoutService.startCheckout`, `VnpayIpnService.handle` | `rollbackFor=Exception` |
| `VnpayExpireService.reconcile` | `REQUIRES_NEW` per txnRef |
| `Job004.releaseExpiredVietQrReservations` | One tx for the whole batch |
| `PaymentTransactionLogService.save` | Joins caller tx (failure logs rolled back with the caller) |
| `AuthenticationEventListeners` | `@Transactional` on a logging-only listener |

## 5. Concurrency & locking

- `PESSIMISTIC_WRITE`: `VariationRepository.findAllByIdInForUpdate` (ordered by id — deterministic lock order), `VnpayPaymentRepository.findByTxnRefForUpdate`.
- `synchronized`: `VietQrApiClient.getAccessToken` (token cache), `ProductImageCompressionService` task map.
- No `@Version` (no optimistic locking) anywhere.
- In-memory state (lost on restart, not cluster-safe): `OtpVerificationService` OTP/rate-limit maps, `ContextRepository` security contexts (`ContextService.storesSecurityContextInRedis` is a misnomer — no Redis).
- Existing race risks (document only, do not fix in refactor):
  1. VietQR webhook: no lock on `CheckoutPending`/`Order`; duplicate concurrent webhooks may double-create payment / double-schedule side effects.
  2. Job004 / `resolveCheckoutStatus` read Order without lock → may overwrite a concurrently committed `PENDING_SYNC` with `CANCELLED`.
  3. Order code from `order_sequence` read-then-increment, no lock; uniqueness constraint UNKNOWN.
  4. Pancake stock webhook / daily sync overwrite `remainQuantity` absolutely, ignoring local HELD deductions; exceptions swallowed.
  5. Job002, async sync and AM006 retry can overlap; only guard is `pancakeOrderId != null`.
  6. External HTTP calls inside DB tx while holding row locks (VietQR QR generation after `reserve`; VNPay QueryDR under `vnpay_payment` lock).

## 6. JPA runtime behavior to preserve

- `ddl-auto=update` (both profiles) — any entity mapping change alters the DB schema on deploy.
- `BaseEntity` + `AuditingEntityListener` on 16 entities.
- EAGER `@ManyToOne` defaults (Cart, CartItem, Order, OrderItem, Payment `@OneToOne`); `cascade=ALL` on `Order.orderItems`, `Cart.cartItems`, `YProduct` collections.
- Generator-name mismatch in `MailHistory`, `YProduct` → effective sequence allocation UNKNOWN; do not rename.
- `spring.jpa.open-in-view` is set in `application.properties` (value not inspected; affects lazy loading in views — Phase 3).
