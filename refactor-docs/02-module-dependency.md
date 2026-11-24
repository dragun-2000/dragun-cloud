# 02 — Module & Dependency Map

Paths relative to `src/main/java/vn/co/cake/`. Baseline `a0515d8`.

## 1. Package structure

| Package | Role | Notes |
|---|---|---|
| `Application` | `@SpringBootApplication`, `@EnableScheduling`, `@EnableConfigurationProperties(OpenAIConfig)` | Default component scan of `vn.co.cake.**` |
| `controller/AM001..AM011`, `controller/system` | Admin screens (`/AM/**`) | Extend `BaseController`; pattern to copy: `AM010Controller` |
| `controller/SA001, SA002` | Customer login/register/OTP/password | `/SA/**` |
| `controller/store` | Storefront: `HomeController`, `CartController`, `CustomerOrderCancelController` | Not covered by any security chain |
| `controller/external` (+`dto`) | `/api/extend/**` CRUD, `/api/v2/pancake/**` webhook | No auth |
| `controller/thiyen` | `/api/thiyen/products` CRUD | No auth; standalone "thiyen" product model |
| `controller/LocationController`, `BaseController` | Province/district/ward JSON; shared session/model helpers | |
| `service`, `service/impl` | Account, Cart, Order, Product, Variation, CustomerOrderCancel, PixelConversion | |
| `service/external` | `PancakePosService` + Extend CRUD services, `PancakeOrderStatus`, `PancakeOrderSyncWriter` | |
| `service/aws` | `S3ServiceImpl` — **local filesystem**, not S3 | Misleading name |
| `service/image` | Image compression (upload + batch product) | Own thread pools |
| `service/mail` | `OrderConfirmationMailService` (`@Async`) | |
| `service/thiyen` | Thiyen product CRUD | |
| `payment/**` | VietQR + COD checkout, inventory reservation, payment logs, pilot access, after-commit schedulers | Shared by VNPay |
| `vnpay/**` | VNPay checkout, IPN/return, QueryDR reconcile job, signer | Depends on `payment` (reservation, logs, schedulers) |
| `ai/**` | OpenAI size advice | |
| `security/**` | Admin & user filter chains, UserDetailsServices, session context repos, auth listener, `JwtUtil` (unused) | |
| `entity`, `entity/external`, `entity/thiyen`, `payment/entity`, `vnpay/entity` | JPA entities | Status fields are `String`, no `@Enumerated` |
| `repository`, `repository/impl`, `repository/thiyen`, `payment/repository`, `vnpay/repository` | Spring Data + QueryDSL custom impls | |
| `dto`, `request`, `response`, `payment/dto`, `vnpay/dto`, `ai/dto`, `controller/external/dto` | DTOs | Several near-duplicates (see 05 §refactor candidates) |
| `common`, `constants`, `enums`, `utils`, `helper` | Constants (two overlapping packages), enums (string constants only), utils, `EmailService`, `SmsService`, `CSVHelper` | |
| `config` | Web, async pool, auditing, RestTemplates, mail template engine, image props, startup runners | |
| `handler` | `GlobalExceptionHandler` | |
| `job` | `Job01`, `Job002`, `Job003`, `Job004` | |

## 2. Layer dependencies (main edges)

```
Controllers ──► Services ──► Repositories ──► Entities
     │              │
     │              ├──► payment/InventoryReservationService ◄── vnpay/*, payment/*, AM006, Job004
     │              ├──► payment/PaymentTransactionLogService ◄── VietQR, VNPay, AM006, AM010
     │              ├──► payment/*Scheduler (after-commit) ──► PancakePosService (@Async)
     │              │                                      ──► OrderConfirmationMailService (@Async) ──► EmailService (Brevo)
     │              │                                      ──► EmailService (admin alert, sync)
     │              └──► PancakePosService ──► Pancake REST API
     └──► Repositories directly (AM003, AM004, AM006, AM009, CartController, HomeController, LocationController, SizeAdviceController)
```

**Shared / high-blast-radius components** (any change = HIGH/CRITICAL risk):
`OrderServiceImpl`, `InventoryReservationService`, `PaymentTransactionLogService`, `PancakePosService`, the three after-commit schedulers, `BaseController`, `GlobalExceptionHandler`, `EmailService`, `BaseEntity`, `VariationRepository` (pessimistic lock), `OrderRepository` (native queries).

**Cross-module coupling:** `vnpay` → `payment` (reservation, logs, schedulers, alert); `controller/AM006` → order + Pancake + reservation + logs; `CartController` → 10+ collaborators (cart, order, Pancake, VietQR props, pilot access, 4 repositories).

## 3. Security filter chains (verified)

| Order | Class | Matches | Rules |
|---|---|---|---|
| -1 | `vnpay/config/VnpaySecurityConfiguration` | `/vnpay/**` | permitAll, CSRF ignored |
| 0 | `payment/config/VietQrSecurityConfiguration` | `/vqr/**` | permitAll, CSRF ignored (transaction-sync checks Bearer itself) |
| 1 | `security/user/UserSecurityConfiguration` | `/SA/**`, `/csv/**` | `/SA/SA001/**`, SA002 reset flow, `/error` public; `/SA/**` ROLE_USER; form login `/SA/SA001/login`; remember-me 15d |
| 2 | `security/admin/AdminSecurityConfiguration` | `/AM/**`, `/HO/**`, `/csv/**` | `/AM`, `/AM/AM001/**` public; `/AM/system/**` ADMIN; `/AM/**` ADMIN/STAFF; `/HO/**` MANAGE |

- **Unmatched URLs pass through no security chain** (no auth, no CSRF): `/`, `/products*`, `/cart-detail`, `/add-to-cart`, `/payment*`, `/order-history`, `/api/**`, `/provinces`, `/districts/*`, `/wards/*`, `/setup/location`, `/privacy-policy`, `/variations/{id}` (PUT in AM003Controller without `/AM` prefix).
- Some of these check the session manually (`CartController`, `CustomerOrderCancelController`, payment/VNPay APIs, `SizeAdviceController`); `controller/external/*`, `controller/thiyen/*`, `PancakeController`, `LocationController` do not.
- Dead rules: `/csv/**` in admin chain (shadowed by order 1); `/HO/**` in user chain; admin chain's `/` and `/SA/SA001/register` permits.
- `/AM/AM001/**` permitAll exposes `/AM/AM001/change-password` and `change_password_view/{id}` anonymously at filter level (controller logic UNKNOWN — Phase 3).

## 4. Third-party dependency usage (grep of imports — candidates only)

| Dependency | Used? |
|---|---|
| querydsl, poi-ooxml, httpclient, java-jwt (`VietQrPartnerTokenService`), facebook-java-business-sdk (`PixelConversionServiceImpl`), commons-lang3 | USED |
| io.minio, opencsv, super-csv, commons-csv, modelmapper, aws-java-sdk-ses, aws-secretsmanager-jdbc, spring-security-oauth2-autoconfigure, bouncycastle, commons-io, okhttp, javax.mail | **No source references** — may still be needed transitively (e.g. POI ↔ commons-io/bouncycastle, minio ↔ okhttp). Removal = dependency change → approval + build verification required. |
| spring-boot-starter-mail | No code use, but auto-configures a `JavaMailSender` from `spring.mail.*` |
| Duplicate declarations | `jackson-core` ×2, `okhttp` (explicit + excluded transitive), bouncycastle explicit pinned |
| Version skew | Thymeleaf starter pinned `2.1.2.RELEASE` under Boot 2.5 parent; `jackson-core 2.9.8` pinned then re-declared unversioned — effective version UNKNOWN (needs `mvn dependency:tree`, Phase 4) |
