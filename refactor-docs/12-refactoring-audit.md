# 12 — Refactoring Audit

Baseline `a0515d8`. Paths relative to `src/main/java/vn/co/cake/`. **Nothing in this document has been implemented.** Each finding is a candidate; implementation needs owner approval per task (`13-refactoring-plan.md`).

**Regression baseline:** `mvn -B test` → 297 run, only BF-01 failing (`10-baseline-results.md` §6).

## 0. Method and evidence

| Check | How | Result |
|---|---|---|
| Unused imports | Script: each import's simple name searched in comment-stripped file body (`scratchpad/unused_imports.py`) | 14 in 10 files |
| Wildcard imports (forbidden by `docs/coding_rule.md` §2) | `grep '^import (static )?….\*;'` | 40 lines in 34 files |
| Dead-code candidates | `grep -w` over `src/main` + `src/test` (Java, HTML, JS, properties, XML); Spring annotations inspected; templates/JS checked for URLs | see category C |
| Duplicates | Bodies compared with `diff` | 1 truly identical pair; 4 "truncate" helpers **not** equivalent |
| Commented-out code | `grep '^\s*//.*[;{}]\s*$'` | 60 lines (28 in `ProductServiceImpl`, 19 in `PaymentCheckoutController`) |
| Unused locals | No tool configured (javac does not report them); found by reading only | partial — see B |
| Prior phases | 02, 05, 06, 09 (U-01…U-60) | reused |

**Verification methods used in this audit**

- **VM-BYTE — bytecode equivalence:** compile before and after, then compare `javap -c -p -constants` output of every touched class. Line numbers aren't printed without `-l`, so this ignores them. For import-only or comment-only changes, the output must be identical. This is the strongest evidence available without a running application.
- **VM-SUITE:** the full suite must still show 297 run with only BF-01 failing (plus any newly approved tests).
- **VM-GREP:** zero remaining references after a removal.
- **VM-CTX:** some changes alter the Spring bean set or repository composition. Those can't be fully verified without starting the application context, which needs a DB (Tier C). Without one they stay **UNVERIFIED at runtime**.

## 1. Findings summary

| ID | Cat | Title | Risk | Recommendation |
|---|---|---|---|---|
| A-01 | A | 14 unused imports in 10 files | LOW | Do (wave 1) |
| A-02 | A | 40 wildcard imports in 34 files (coding-rule violation) | LOW | Do (wave 1, split by package) |
| B-01 | B | Unused local `url` in `UserLogoutSuccessHandler` | LOW | Do (wave 1) |
| B-02 | B | Write-only field `authentication` in `UserContextRepository` (singleton, mutated per request) | LOW | Do (wave 2) |
| C-01 | C | Empty `AM005Controller` | LOW | Do (wave 1) |
| C-02 | C | `helper/CSVHelper` unreferenced | LOW | Do (wave 1) |
| C-03 | C | 60 lines of commented-out code | LOW | Do (wave 1, excluding the VietQR sandbox block, which needs an owner decision) |
| C-04 | C | 11 unreferenced Spring Data repository methods | LOW | Do (wave 1) |
| C-05 | C | Unreferenced public service methods (`CartService.findOtherProductItem`, `PancakePosService.enrichOrdersFromPancake`, `VietQrCheckoutService.getCheckoutStatus`) | LOW–MEDIUM | Do (wave 2) |
| C-06 | C | `PixelConversionServiceImpl` (placeholder `@Service`, no callers) | MEDIUM | Do (wave 2) |
| C-07 | C | Empty `CustomCartRepository` / `CustomCartRepositoryImpl` fragment | MEDIUM | Wave 2, VM-CTX caveat |
| C-08 | C | `WebhookHistoryRepository` unreferenced | MEDIUM | Defer (ties to U-28; history save is commented out) |
| C-09 | C | `JwtUtil` unreferenced, but it carries `@PropertySource` and a hardcoded secret | MEDIUM | Owner decision |
| C-10 | C | Template `SA001/SA001.html` unreferenced | LOW–MEDIUM | Wave 2 |
| C-11 | C | `runSandboxPaymentConfirm` reachable only from a commented-out endpoint | MEDIUM | Owner decision |
| D-01 | D | `normalizeStoreUrl` + `DEFAULT_STORE_URL` byte-identical in 2 classes | LOW | Do (wave 2) |
| D-02 | D | 20 copy-pasted image-upload blocks in `AM003Controller` | MEDIUM | Wave 3 (needs Tier B controller test) |
| D-03 | D | `OpenAIServiceImpl.callGPT` / `callGPTWithHistory` duplicate request + catch ladder | MEDIUM | Wave 3 (needs Tier B test) |
| D-04 | D | Payment flow duplication (VietQR vs VNPay paid flow, expiry ×3, draft creation ×2, `create` vs `createWithOrderCode`, grand total ×4, after-commit schedulers ×3) | CRITICAL | Defer (needs Tier B + C) |
| D-05 | D | 4 "truncate" helpers | — | **REJECTED**: not equivalent (see card) |
| D-06 | D | Near-duplicate DTO pairs (VietQR/VNPay pilot DTOs, `DistrictDto`≈`WardDto`, external DTO copies) | MEDIUM | Defer (JSON contracts) |
| E-01 | E | Unnecessary temporary variables | LOW | No systematic finding; handle opportunistically only inside approved tasks |
| F-01 | F | Very large classes: `SizeAdviceServiceImpl` (1054), `PancakePosService` (908), `AccountServiceImpl` (608), `AM003Controller` (578), `CartController` (560) | HIGH | Defer |
| G-01 | G | Service interface + single implementation pattern | — | Keep (project convention) |
| H-01 | H | `S3ServiceImpl` writes to local disk (misleading name) | MEDIUM | Wave 3 |
| H-02 | H | `ContextService.storesSecurityContextInRedis` is in-memory, not Redis | LOW–MEDIUM | Wave 2 |
| H-03 | H | Config prefix `threadpoll.*`, `speedsms.otp.*` | — | Out of scope (config change) |
| H-04 | H | `dto.OrderItem` vs `entity.OrderItem` name clash | HIGH | Defer |
| H-05 | H | Inconsistent job names `Job01`/`Job002`/`Job003`/`Job004` | LOW–MEDIUM | Not recommended (low value) |
| I-01 | I | Unreachable branches: `SA001Controller:204-207`, `CartServiceImpl:110` | MEDIUM | Wave 3 (needs tests) |
| I-02 | I | Double `setKeepAliveSeconds` in `ThreadPoolManager` | — | **Do not touch** (first call enforces a required property) |
| J-01 | J | `new ObjectMapper()` per call (15 sites, 9 inside `PancakePosService`) | MEDIUM | Not recommended now (see card) |
| K-01 | K | Performance: N+1 product lookup in subtotal; Job01 `saveAll` on all products every minute; synchronous Pancake calls on `/order-history` | HIGH–CRITICAL | Document only (query/behavior change) |
| L-01 | L | Architecture/security: missing filter chains, `ddl-auto=update`, `@Async` executor, single scheduler thread, in-memory OTP/context, secrets in source and logs | CRITICAL | Separate tickets, not this refactor |
| L-02 | L | `printStackTrace` ×7 / `System.out.println` ×1 | LOW | Owner decision (changes stdout/stderr output) |
| M-01 | M | Risky or unclear business logic U-01…U-60 | CRITICAL | No refactoring; owner triage |

## 2. Finding cards

### A-01: Unused imports
- **Files:**
  - `controller/BaseController.java`: `java.util.HashMap`, `java.util.List`, `exception.CommonServletException`
  - `dto/GenericMailForm.java`: `java.util.List`
  - `entity/Account.java`: `dto.CartForm`
  - `entity/Cart.java`: `org.springframework.util.CollectionUtils`, `dto.CartForm`, `dto.OrderItem`
  - `enums/Colors.java`: `lombok.Data`
  - `job/Job004.java`: `java.util.List`
  - `payment/api/VietQrTransactionSyncController.java`: `payment.PaymentConstants`
  - `service/external/OrderExtendService.java`: `dto.GenericMailForm`
  - `service/impl/CartServiceImpl.java`: `java.math.BigDecimal`
  - `service/impl/ProductServiceImpl.java`: `controller.external.dto.ProductPancakeRequest`
- **Problem:** noise, false coupling between packages (e.g. entities importing DTOs).
- **Proposed:** delete those import lines only.
- **Benefit:** readability; removes misleading entity→DTO dependencies.
- **Dependency impact:** none (imports are compile-time only).
- **Behavior preservation:** bytecode must be identical.
- **Risk:** LOW.
- **Tests:** none new. **Verification:** VM-BYTE on the 10 classes, then VM-SUITE.
- The script is heuristic, so the compiler must confirm. If a removed import was actually needed, compilation fails, so the check fails safe.

### A-02: Wildcard imports (`docs/coding_rule.md` §2)
- **Files** (34): `entity` 11, `service/impl` 6, `service/external` 3, `entity/thiyen` 3, `controller/store` 3, `controller/external` 3, `service/aws/impl` 2, plus 1 each in `utils`, `entity/external`, `controller/thiyen`, `controller/external/dto`, `controller/SA001`, `controller/AM009`, `controller/AM003`, `ai/service/openai`.
- **Problem:** violates the mandatory project rule; hides dependencies.
- **Proposed:** replace each wildcard with the explicit imports actually used.
- **Behavior-preservation trap:** a single-type import **shadows** a same-package class, while an on-demand (`*`) import does **not**. Example: `vn.co.cake.dto.OrderItem` vs `vn.co.cake.entity.OrderItem`. A careless explicit import can silently re-bind a simple name to a different class, and this is not always a compile error when both classes share signatures. That is why VM-BYTE is mandatory: constant-pool class references must not change.
- **Risk:** LOW, given VM-BYTE. **Split:** one task per package group (4 sub-tasks) to keep diffs reviewable.
- **Tests:** none new. **Verification:** VM-BYTE + VM-SUITE.

### B-01: Unused local `url`
- **Location:** `security/user/UserLogoutSuccessHandler.java:40`. A `String.format("%s%s?logout", …)` result is never used.
- **Proposed:** delete the statement.
- **Behavior:** `String.format` has no side effects and its arguments are field reads. Bytecode changes (the call disappears); observable behavior does not.
- **Risk:** LOW. **Verification:** VM-SUITE plus review. VM-BYTE is expected to differ only by that removed call sequence.

### B-02: Write-only field in a singleton
- **Location:** `security/repository/UserContextRepository.java:20,32`. The field `authentication` is assigned on every request and never read. It's a shared mutable field in a singleton bean.
- **Proposed:** remove the field and the assignment. Keep the `securityContext.getAuthentication()` call only if it has side effects; it doesn't.
- **Risk:** LOW, but it sits in the security layer. Under the approval gates it counts as a shared component, so it has its own task.
- **Verification:** VM-SUITE + review.

### C-01: `AM005Controller`
- An empty `@Controller @Slf4j` class extending `BaseController`. It has no mappings. Zero references in Java, templates or JS.
- **Removal effect:** one fewer bean, no endpoints lost.
- **Risk:** LOW. **Verification:** VM-GREP + VM-SUITE. Context load is VM-CTX, but there's nothing to break.

### C-02: `helper/CSVHelper`
- A plain class (not a bean) with static `TYPE`, `HEADER` and `hasCSVFormat`. Zero references.
- **Risk:** LOW. **Verification:** VM-GREP + compile + VM-SUITE.

### C-03: Commented-out code (60 lines)
- `service/impl/ProductServiceImpl.java`: 28 lines, including the `createProduct` Pancake call.
- `payment/api/PaymentCheckoutController.java:125-146`: 19 lines, the **VietQR sandbox-simulate endpoint**.
- A few elsewhere: `AM003Controller:519-534` validation + apply discount; `AccountServiceImpl` mail pushes; `PancakePosService:124` history save.
- **Problem:** obscures the real behavior.
- **Proposed:** delete. **Exception:** blocks that document a disabled feature the owner may re-enable stay until the owner decides: the sandbox endpoint (C-11), the AM003 discount apply (U-38), and the admin reset email (U-33).
- **Risk:** LOW (comments only). **Verification:** VM-BYTE (must be identical) + VM-SUITE.

### C-04: Unreferenced Spring Data methods
- `AccountRepository.findAllByAuthoritiesAndMailAddressIn`
- `CustomAccountRepository.findIdAllByDeletedIsFalseAndFullNameContaining` (plus its impl)
- `CartRepository.findFirstCartByCartId`
- `CategoryRepository.findFirstByName`
- `OrderRepository.findByPhoneAndDeletedFalseOrderByCreatedDesc`
- `OrderRepository.findOrdersNeedingPancakeEnrichment` (native)
- `ProvinceRepository.findAllByOrderByNameAsc`
- `VariationRepository.findFirstByPancakeProductIdAndSize`
- `CheckoutPendingRepository.findFirstByVietqrOrderIdAndStatus`
- `InventoryReservationRepository.findFirstByOrderIdAndVariationId`
- `InventoryReservationRepository.releaseHeldByOrderId` (`@Modifying`)
- **Evidence:** 0 references outside repository declarations. Derived query methods can only be called from Java.
- **Removal effect:** Spring Data no longer derives or validates these queries at startup. No runtime path uses them.
- **Risk:** LOW. `findOrdersNeedingPancakeEnrichment` goes together with C-05 (`enrichOrdersFromPancake`).
- **Verification:** VM-GREP + VM-SUITE; VM-CTX UNVERIFIED (removal can only reduce startup work).

### C-05: Unreferenced public service methods
- `CartService/CartServiceImpl.findOtherProductItem` (interface + impl). It also contains a boxed `!=` comparison bug.
- `PancakePosService.enrichOrdersFromPancake` (public, `@Transactional`).
- `VietQrCheckoutService.getCheckoutStatus` (public, `@Transactional`, writes).
- **Evidence:** 0 callers. These aren't endpoints, have no reflection, no Thymeleaf or JS use, and no `@Scheduled` or `@EventListener`.
- **Risk:** LOW–MEDIUM, because the classes are payment/integration components (approval gate 7). Removing unused methods doesn't change any executed path.
- **Verification:** VM-GREP + VM-SUITE.

### C-06: `PixelConversionServiceImpl`
- A `@Service` with placeholder token and pixel id, sample hardcoded event data, and `printStackTrace`. 0 callers.
- **Removal effect:** one bean fewer. Its `facebook-java-business-sdk` dependency becomes unused; removing that from `pom.xml` is a separate dependency task, not proposed here.
- **Risk:** MEDIUM (bean removal, external SDK). **Verification:** VM-GREP + VM-SUITE; VM-CTX UNVERIFIED.

### C-07: Empty custom repository fragment
- `repository/CustomCartRepository` (empty interface) and `repository/impl/CustomCartRepositoryImpl` (empty). `CartRepository extends JpaRepository<Cart, Long>, CustomCartRepository`.
- **Removal effect:** changes how the Spring Data repository is composed; the fragment contributes no methods.
- **Risk:** MEDIUM (repository wiring, VM-CTX UNVERIFIED without a DB).

### C-08: `WebhookHistoryRepository`
- 0 references. `PancakePosService:96-124` builds a `WebhookHistory` but the save call is commented out (U-28).
- **Recommendation:** defer. Removing it closes off the obvious fix for U-28; owner decision.

### C-09: `security/utils/jwt/JwtUtil`
- An `@Configuration` + `@PropertySource("classpath:application.properties", ignoreResourceNotFound=true)` with static `verifyToken`/`decodedJWT` and a **hardcoded JWT secret literal** (add to R-01). 0 callers.
- **Removal trap:** removes an extra `@PropertySource` registration of `application.properties`. The effect on property resolution is probably nil, because Boot already loads it at higher precedence, but that's **UNVERIFIED**.
- **Recommendation:** owner decision. If removed, verify with an application context start (Tier C).

### C-10: Template `SA001/SA001.html`
- `ScreenPathConst.SA001_SCREEN` is `"login"`. No Java, template or JS reference to `SA001/SA001`.
- **Risk:** LOW–MEDIUM. Templates are resolved by string, so references could be built dynamically; none were found.

### C-11: `VietQrCheckoutService.runSandboxPaymentConfirm`
- Its only reference is the commented-out endpoint in `PaymentCheckoutController:138`.
- **Recommendation:** owner decision, together with C-03's sandbox block. Keep both, or remove both.

### D-01: Identical `normalizeStoreUrl`
- `helper/EmailService.java:37,128-137` and `service/mail/OrderConfirmationMailService.java:41,163-172` are byte-identical (verified with `diff`), including `DEFAULT_STORE_URL = "https://debase.vn"`.
- **Proposed:** one `public static` method plus constant in an existing utility (`utils/StringUtil`, no new class). Both services call it.
- **Dependency impact:** `StringUtil` is a shared component (approval gate 4).
- **Behavior:** identical code path.
- **Tests:** add characterization tests for the new public method (blank, null, trailing slashes, missing scheme, http/https) before moving it.
- **Risk:** LOW. **Verification:** new test + VM-SUITE.

### D-02: `AM003Controller` image-upload blocks
- 10 blocks at `:244-285` (register) and 10 more at `:414-455` (update). Each is `if (fileN != null && !fileN.isEmpty()) { url = s3Service.uploadImageToS3(fileN); request.setImageNUrl(url); }`.
- **Proposed:** one private helper inside the same controller. Upload order (file, file1…file9) and per-file exception propagation must stay the same.
- **Risk:** MEDIUM (controller has no tests; file I/O). **Tests required:** Tier B controller-level test with mocked `S3Service`/`ProductService`, asserting call order and the URL set on each setter.

### D-03: `OpenAIServiceImpl` duplication
- `callGPT` (`:40-118`) and `callGPTWithHistory` (`:121-208`) repeat the request build and the 4-level catch ladder: `ResourceAccessException`, `HttpClientErrorException` (429/401 mapping), `RestClientException`, `Exception`.
- **Proposed:** a shared private method for the POST + error mapping.
- **Behavior:** exception types and messages must be identical. `SizeAdviceServiceImpl` relies on the `/chat` path rethrowing.
- **Risk:** MEDIUM. **Tests required:** Tier B `OpenAIServiceImplTest` with a mocked `RestTemplate` covering each catch branch for both methods.

### D-04: Payment/checkout duplication
- Listed in `05 §10` and `09`. It touches transactions (REQUIRES_NEW, rollbackFor, checked-exception commit semantics U-01), locks, after-commit hooks and idempotency.
- **Risk:** CRITICAL. **Recommendation:** out of scope until Tier B (CT-B1…B4) and Tier C exist; then raise as a separate proposal.

### D-05: Truncate helpers (REJECTED)
- `SmsService.truncateForLog` returns `"null"` for null and strips whitespace.
- `PaymentTransactionLogService.truncatePayload` treats empty input as a passthrough.
- `PaymentTransactionLogService.truncate` returns null unchanged.
- `PancakePosService.truncateMessage` appends `"..."` and has a `maxLen <= 3` passthrough.
- **Conclusion:** they behave differently, so consolidating would change outputs (log text, `message_error` column). No action.

### D-06: Near-duplicate DTOs
- These DTOs are serialized as API request/response bodies. Merging them risks JSON contract changes (field names, unknown-property handling).
- **Recommendation:** defer. Needs API contract tests (Tier C MockMvc).

### F-01: Large classes
- Splitting moves `@Transactional`/`@Async` boundaries and self-invocation semantics (U-27). **HIGH**, deferred.
- The only safe sub-step is extracting pure private helpers, and only when covered by tests (e.g. `SizeAdviceServiceImpl` now has 50 characterization runs). Not proposed in this round.

### G-01: Interfaces with a single implementation
- `AccountService`, `CartService`, `OrderService`, `ProductService` and others: 6 service interfaces in `service/`.
- This is the project's established convention, and removing them changes the bean types used for injection. **Keep.**

### H-01: `S3ServiceImpl` / `S3Service` naming
- Writes to the local filesystem. Renaming changes the bean name (`s3ServiceImpl`) and touches the interface, `AM003Controller`, `ProductExtendController` and `S3ServiceImplTest`.
- Injection is by type, so wiring is unaffected; any by-name lookup would break (none found).
- **Risk:** MEDIUM. Wave 3, optional.

### H-02: `ContextService.storesSecurityContextInRedis`
- Stores in the in-memory `ContextRepository`. Rename to e.g. `storeSecurityContext`. Callers are in the security handlers only.
- **Risk:** LOW–MEDIUM (security package; rename only). **Verification:** compile + VM-SUITE + VM-GREP.

### H-03: Misspelled or legacy config prefixes
- `threadpoll.*` and `speedsms.otp.*`. Renaming them requires a config change in every environment. **Out of scope.**

### H-04: `OrderItem` name clash
- The DTO and the entity share a simple name, used across many files. HIGH churn and a wildcard-import shadowing risk (A-02). **Deferred.**

### I-01: Unreachable branches
- `SA001Controller:204-207`: the "expired" and "too many attempts" messages can't be reached (Phase 3, `06` F13).
- `CartServiceImpl:110`: a null check after `groupingBy`, which already threw an NPE.
- Removing dead branches doesn't change executed paths, but neither area has tests: there's no SA001 controller test, and `CartServiceImpl` isn't covered.
- **Risk:** MEDIUM. Needs Tier B tests first.

### I-02: `ThreadPoolManager` double keep-alive (DO NOT TOUCH)
- `:25` calls `env.getRequiredProperty("threadpoll.keepAliveSeconds", Integer.class)`, which throws at startup if the property is missing. `:29` then overwrites the value with 30.
- Deleting `:25` would remove that startup failure mode, which is a behavior change. Recorded as a trap.

### J-01: `new ObjectMapper()` per call
- 15 sites: `PancakePosService` ×9, `CartController`, `HomeController`, `EmailService`, `SmsService`, `SizeAdviceServiceImpl`, `OpenAIServiceImpl`, `MasterUtil`.
- **Trap:** injecting Spring Boot's `ObjectMapper` bean would change behavior. Boot sets `FAIL_ON_UNKNOWN_PROPERTIES=false` (among other settings), while U-28 depends on the default `true` in `updateStock`.
- **The only safe variant:** a `private static final ObjectMapper` per class with default configuration. The gain is negligible (JSON on the network path).
- **Recommendation:** not now (low benefit, non-zero risk).

### K-01: Performance
- N+1 in `OrderServiceImpl.calculateItemsSubtotal`; `Job01` loading and `saveAll`-ing every product every minute; synchronous Pancake GETs during `/order-history`.
- All fixes change query semantics or side effects. **Document only**; separate performance tickets.

### L-01: Architecture and security
- See `01 §6` R-01…R-15 and `09` U-24, U-30…U-36. **Out of scope** for a behavior-preserving refactor. Recommended as prioritized tickets.

### L-02: `printStackTrace` / `System.out.println`
- `printStackTrace`: `AdminContextRepository:31`, `UserContextRepository:35`, `JwtUtil:26,37`, `PancakePosService:156,764`, `PixelConversionServiceImpl:55`.
- `System.out.println`: `PancakePosService:145`, which prints the Pancake product request JSON.
- Replacing these with SLF4J changes where and what is written: stderr/stdout vs the log file, format and level.
- **Owner decision.** If approved: LOW risk, one task.

### M-01: Risky or unclear business logic
- U-01…U-60. **No refactoring.** Each needs an owner decision (intended or bug) and, if it's a bug, its own fix ticket with tests.

## 3. Coding-rule compliance (`docs/coding_rule.md`)

| Rule | Current state | In plan? |
|---|---|---|
| §2 no wildcard imports | 40 violations / 34 files | Yes, A-02 |
| §1 UPPER_SNAKE constants, no magic literals | Many literals (status strings, Vietnamese messages, URLs) | No. Mass churn; only within files touched by approved tasks, and only where the literal is not part of an external contract |
| §7.1 `String.format` over `+` | Widespread `+` | No. Same reason |
| §7.2 spelling | `threadpoll`, `deleted` method names, etc. | Only H-02 |
