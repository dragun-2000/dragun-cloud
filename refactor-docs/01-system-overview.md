# 01 — System Overview

Baseline: `a0515d8` on `dev/refactor_20261123`. Evidence gathered by static reading (grep/read); nothing was executed. `UNKNOWN` = not verified.

## 1. What the system is

Server-rendered e-commerce store ("Debase" / Maven artifact `vn.co.cake:cake`) with:
- **Storefront** (anonymous + customer `SA*` screens): product catalog, cart, checkout (COD, VietQR, VNPay), order history, customer cancel, AI size advice.
- **Admin back office** (`AM*` screens): accounts, products/variations/vouchers, orders + Pancake sync retry, provinces, payment logs, payment pilot settings, image compression.
- **Integrations**: Pancake POS (orders/products/stock), VietQR, VNPay, Brevo email, TingTing SMS, OpenAI, Facebook Pixel conversion API, local-disk image storage.

## 2. Technology stack (verified from `pom.xml` / config)

| Area | Technology |
|---|---|
| Language / runtime | Java 11 (JDK 11.0.5 locally; Docker `eclipse-temurin:11`) |
| Framework | Spring Boot **2.5.13** (Web MVC, Security 5.5 `WebSecurityConfigurerAdapter`, Data JPA, Thymeleaf, Mail starter) |
| Persistence | PostgreSQL 15, Hibernate (via Boot), HikariCP (`spring.datasource.type`), QueryDSL 4.4.0 (APT via `apt-maven-plugin`) |
| Schema management | `spring.jpa.hibernate.ddl-auto=update` in **both** dev and production profiles; `init/init.sql` + `init/migration_*.sql` (run by Postgres container init) |
| Views | Thymeleaf + layout dialect + springsecurity5 extras; Bootstrap/jQuery static assets |
| HTTP clients | `RestTemplate` + Apache HttpClient 4.5 |
| Build | Maven (no wrapper in repo). Packaging: fat jar `cake-1.0-SNAPSHOT.jar` |
| Deploy | `Dockerfile` (multi-stage, `-DskipTests`), `docker-compose.yml` (app + postgres), `Jenkinsfile` (build → docker build → `docker compose up`; **test stage commented out**) |
| Active profile | `spring.profiles.active=production` by default (`application.properties:2`) |
| **Not used** (despite docs) | Redis, Spring Session, MinIO, AWS S3/SES SDK — see `05-external-integrations.md` §9 |

## 3. Size

- 308 main Java files, ~25.2k LOC, base package `vn.co.cake`.
- 10 test files (image compression, Pancake status mapping, VNPay signer/formats/IPN, S3 path). No Spring context tests, no controller tests, no repository tests.
- Largest classes: `ai/service/impl/SizeAdviceServiceImpl` (1054), `service/external/PancakePosService` (908), `service/image/ImageCompressor` (746), `service/impl/AccountServiceImpl` (608), `service/image/ProductImageCompressionService` (592), `controller/AM003/AM003Controller` (578), `controller/store/CartController` (560).

## 4. Build & test commands (identified, NOT yet run)

```
mvn -B clean package -DskipTests      # Dockerfile / Jenkins build
mvn -B test                           # unit tests (Phase 4)
```
`mvn` is not on PATH; use the cached Maven 3.9.9 under `%USERPROFILE%\.m2\wrapper\dists\...\bin\mvn.cmd`. No lint/format/static-analysis tool is configured (no Checkstyle/PMD/SpotBugs/JaCoCo/Sonar in pom). No `src/test/resources`.

## 5. Existing project instructions

- No `CLAUDE.md`.
- `docs/coding_rule.md` (v1.2, "Priority: Highest"): UPPER_SNAKE constants, no magic literals, **no wildcard imports**, `String.format` over `+`, preserve behavior. Must be followed for any refactor edit.
- `PROJECT_DOCUMENTATION.md` — **partly stale** (claims Redis 7, Spring Session, MinIO, SendGrid; none are in the code).
- `CODE_REVIEW_REPORT.md` — older province-feature review.
- `docs/*.md` — VietQR/VNPay design notes, Pancake 443 analysis.

## 6. Risks (Phase 1–2)

Severity is about the *existing system*; none of these are to be fixed inside the refactor without explicit approval (they are behavior changes).

| ID | Risk | Evidence | Severity |
|---|---|---|---|
| R-01 | Literal secrets committed: `pancake.pos.api.token`, `vietqr.partner.password`, `vietqr.partner.jwt-secret` (dev profile), `tingting.api.key` (base), DB password in `docker-compose.yml`, admin seed password in `config/LoadDatabase.java` | key classification run on properties (values not printed) | CRITICAL (security, outside refactor) |
| R-02 | VNPay hash secret logged at INFO on every checkout | `vnpay/service/VnpayCheckoutService.java:121-122` (`temp={}` → `getHashSecret()`) | CRITICAL (security) |
| R-03 | Plaintext OTP logged | `controller/SA001/SA001Controller.java:170` | HIGH (security) |
| R-04 | Pancake failure dump (URL incl. `api_key`) written to `log/pancake-curl-*.txt` and emailed in chunks to a **hardcoded personal address**, with `Thread.sleep` between parts | `service/external/PancakePosService.java:670-731` | HIGH (security/ops) |
| R-05 | No security filter chain for `/`, `/products*`, `/cart*`, `/payment*`, `/order-history`, `/api/**`, `/provinces` etc. `/api/extend/**`, `/api/thiyen/**` (CRUD incl. DELETE), `/api/v2/pancake/webhook`, `/api/v2/pancake/sync-products` have **no auth check at all** | `02-module-dependency.md` §Security | CRITICAL (security, outside refactor) |
| R-06 | `ddl-auto=update` in production — Hibernate mutates the live schema; entity changes in a refactor can alter the DB | `application-production.properties:20` | HIGH (refactor guard: **no entity mapping changes**) |
| R-07 | `@Async` executor: only a `ThreadPoolExecutorFactoryBean` exists (not a `TaskExecutor`, not named `taskExecutor`) → Spring falls back to `SimpleAsyncTaskExecutor` (thread per call, unbounded) | `config/ThreadPoolManager.java:14-33`; no `AsyncConfigurer` found | MEDIUM — runtime UNVERIFIED |
| R-08 | Order/payment concurrency: VietQR webhook has no row lock; Job004 can overwrite a just-paid order with CANCELLED; order-code generation is read-then-increment without lock; Pancake stock webhook overwrites stock ignoring local holds | see `04-runtime-components.md` §Concurrency | HIGH (data integrity, outside refactor) |
| R-09 | Effectively **no test safety net** for order/cart/payment/security flows; CI skips tests | 10 test files; Jenkins test stage commented | HIGH (refactor blocker for those modules until characterization tests exist) |
| R-10 | Branch baseline differs from `develop` by 71 commits; `origin/develop` has 2 commits not here → future merge conflicts | Git analysis | MEDIUM |
| R-11 | Entity ID generator name mismatches (`MailHistory`, `YProduct`) — "fixing" them changes ID allocation | `entity/MailHistory.java:20-21`, `entity/thiyen/YProduct.java:13-14` | MEDIUM (refactor guard) |
| R-12 | `@Transactional` on private methods / self-invocation in `PancakePosService` is ineffective; "cleaning" it up would *change* tx behavior | `PancakePosService.java:163, 525, 639` | HIGH (refactor guard) |
| R-13 | Missing template `SA002/SA002_03.html` referenced by `SA002Controller:64` | template search | LOW (existing bug) |
| R-14 | Static resource path hardcoded `file:/C:/data/external-images/` independent of `image.compression.upload-dir` | `config/WebConfig.java:56-57` | MEDIUM (config coupling) |
| R-15 | Generic exception handler returns `error-page` view with **HTTP 200** for non-API requests | `handler/GlobalExceptionHandler.java:211` | LOW (contract to preserve) |
