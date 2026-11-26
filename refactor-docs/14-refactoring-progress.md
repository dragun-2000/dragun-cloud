# 14 — Refactoring Progress

Approved by owner 2026-11-24: **T-00 + Wave 1 (T-01 … T-07)**, then option A to unblock T-00. Regression baseline: **297 run, 0 failures**; from 2026-11-26, after the owner merged `dev/compression-image` into this branch (`589b5c2`, 26 new tests), **323 run, 0 failures**. VM-BYTE "before" snapshots for T-02d onward are taken from `589b5c2` or later.

| Task | Status | Notes |
|---|---|---|
| BF-01 fix (option A) | **COMPLETED** | `f132489`; gate passed (53 tests) |
| T-00 Commit baseline docs + Tier A tests | **COMPLETED** | Committed in the commit that contains this line (`docs: add refactor baseline …`); gate passed. Was BLOCKED before option A (see below). |
| T-01 Remove unused imports | **COMPLETED** | 14 import lines in 10 files. VM-BYTE identical (11 class files). Gate passed. Committed in the commit containing this line. |
| T-02a Wildcard → explicit imports, `entity/**` | **COMPLETED** | 15 files: `javax.persistence.*` replaced by 101 explicit imports. Verbose VM-BYTE identical apart from line numbers. Gate passed. Committed in the commit containing this line. |
| T-02b Wildcard → explicit imports, `service/**` | **COMPLETED** | 6 files, 11 wildcards (9 packages) → 74 explicit imports. Verbose VM-BYTE identical apart from line numbers; no unused imports. Gate passed. Committed in the commit containing this line. |
| T-02c Wildcard → explicit imports, `controller/**` | **COMPLETED** | 10 files, 11 wildcards → 74 explicit imports. Verbose VM-BYTE identical apart from line numbers (11 class files). Code committed by the owner in `ab72804` ("refactor import *"); this status line and `00` updated in a follow-up docs commit. Suite after the `dev/compression-image` merge (`589b5c2`): 323 run, 0 failures. |
| T-02d Wildcard → explicit imports, `utils`, `ai/**` | **COMPLETED** | 3 files, 3 wildcards → 15 explicit imports. Verbose VM-BYTE identical apart from line numbers (3 class files). No wildcard import left in `src/main/java`. Gate passed (323 run, 0 failures). Committed in the commit containing this line. |
| T-03 Delete commented-out code (partial) | **COMPLETED** | 39 lines in 6 files (36 grep matches + 2 continuation lines + 1 blank). Kept: VietQR sandbox endpoint, AM003 discount validation/apply, admin forgot-password email push. Verbose VM-BYTE identical apart from line numbers (6 class files). Gate passed (323 run, 0 failures). Committed in the commit containing this line. |
| T-04 Delete `AM005Controller` | **COMPLETED** | Empty `@Controller` class and its package dir removed. 0 references. Gate passed (323 run, 0 failures). Committed in the commit containing this line. |
| T-05 Delete `CSVHelper` | **COMPLETED** | Unreferenced plain class removed. 0 references. Gate passed (323 run, 0 failures). Committed in the commit containing this line. |
| T-06 Remove unused local in `UserLogoutSuccessHandler` | **COMPLETED** | Dead `String url = String.format(...)` and the import it alone used removed. VM-BYTE differs only by the removed `String.format` sequence (plus constant-pool renumbering, max stack/locals, LocalVariableTable `url`). Gate passed (323 run, 0 failures). Committed in the commit containing this line. |
| T-07 Remove unreferenced repository methods | **COMPLETED** | 11 repository methods + the matching `CustomAccountRepositoryImpl` method removed; 3 imports that became unused removed. 0 callers. VM-BYTE: only the removed methods (and their constant-pool entries) are gone. Gate passed (323 run, 0 failures). Committed in the commit containing this line. |
| T-07 commit | note | The owner committed T-07 as `e0745ee` ("refactor T07"); that commit also contains an unrelated behavior change in `ProductServiceImpl` (`"color"` field name accepted next to `"Màu"`) and `docs/plan-pancake-price-promotion-sync.md`. |
| **Wave 2 approved** | — | Owner approved T-08 … T-14 on 2026-11-26 (all seven). |
| T-08 Move `normalizeStoreUrl` into `StringUtil` | **COMPLETED** | 3 characterization tests added first (red, then green); method moved verbatim; both mail services call `StringUtil.normalizeStoreUrl`. Gate passed (326 run, 0 failures). Committed in the commit containing this line. |
| T-09 Remove unreferenced public service methods | **COMPLETED** | `CartService/CartServiceImpl.findOtherProductItem`, `PancakePosService.enrichOrdersFromPancake`, `VietQrCheckoutService.getCheckoutStatus` removed (0 callers); 2 imports in `CartService` became unused and were removed. VM-BYTE: only those methods (and the impl's lambda) are gone. Gate passed (326 run, 0 failures). Committed in the commit containing this line. |
| T-10 Delete `PixelConversionServiceImpl` | **COMPLETED** | Unused `@Service` with placeholder token and sample data removed. 0 references. VM-CTX unverified. Gate passed (326 run, 0 failures). Committed in the commit containing this line. |
| T-11 Remove empty `CustomCartRepository` fragment | **COMPLETED** | Empty fragment interface + empty impl removed; `CartRepository` now extends only `JpaRepository<Cart, Long>`. VM-CTX unverified. Gate passed (326 run, 0 failures). Committed in the commit containing this line. |
| T-12 Remove write-only `authentication` field | PENDING | |
| T-13 Rename `storesSecurityContextInRedis` | PENDING | |
| T-14 Delete orphan templates | PENDING | |

## T-01 — Remove unused imports (2026-11-24)

- **Files:**
  - `controller/BaseController` (`HashMap`, `List`, `CommonServletException`)
  - `dto/GenericMailForm` (`List`)
  - `entity/Account` (`dto.CartForm`)
  - `entity/Cart` (`CollectionUtils`, `dto.CartForm`, `dto.OrderItem`)
  - `enums/Colors` (`lombok.Data`)
  - `job/Job004` (`List`)
  - `payment/api/VietQrTransactionSyncController` (`PaymentConstants`)
  - `service/external/OrderExtendService` (`dto.GenericMailForm`)
  - `service/impl/CartServiceImpl` (`BigDecimal`)
  - `service/impl/ProductServiceImpl` (`ProductPancakeRequest`)
- **Diff:** 14 deletions, 0 insertions; nothing else changed.
- **VM-BYTE:** `javap -c -p -constants` of the 10 classes plus 1 nested class, before and after a fresh compile → **identical**.
- **Suite / gate:** see the commit's gate run (`GATE_RESULT=pass`, 297 tests, 0 failures).
- **Behavior impact:** none (compile-time only).
- **Correction (found during T-02a):** removing import lines shifts source line numbers, so stack-trace line numbers in these 10 classes moved up by 1–3. The `javap -c` comparison used here doesn't include debug info, so it couldn't show this. No other effect.

## T-02a — Wildcard → explicit imports in `entity/**` (2026-11-24)

- **Scope:** 15 files, each with exactly one `import javax.persistence.*;`:
  - `entity/`: `CategoryRelations`, `District`, `Order`, `OrderSequence`, `PancakeProperties`, `Product`, `Province`, `Voucher`, `Ward`, `Warehouse`, `WebhookHistory`
  - `entity/external/OrderExtend`
  - `entity/thiyen/`: `ProductGallery`, `ProductVariant`, `YProduct`
- **Resolution method:**
  - The `javax.persistence` type list comes from the only classpath jar that provides the package, `jakarta.persistence-api-2.2.3.jar` (138 types; all 176 classpath jars checked).
  - For each file, the script intersected that list with the capitalized identifiers used in the comment- and string-stripped body.
  - Shadowing checks: no JPA simple name collides with a class in `vn.co.cake.entity`, `.thiyen` or `.external`, with an existing explicit import, or with a type declared in the file.
  - Scripts: `scratchpad/resolve_wildcard.py`, `apply_wildcard.py` (not committed).
- **Diff:** 15 deletions, 101 insertions, all import lines; line endings preserved.
- **VM-BYTE (verbose):** `javap -v -p -constants`, covering annotations and the constant pool, for all 15 classes. The before state was rebuilt from HEAD sources (stashed), the after state from the edit. **Identical** except `LineNumberTable` entries. Stack-trace line numbers in these classes shift by +4…+9; no other effect.
- **Suite / gate:** see the commit's gate run.

## T-02b — Wildcard → explicit imports in `service/**` (2026-11-24)

| File | Wildcards replaced | Explicit imports |
|---|---|---|
| `service/aws/impl/S3ServiceImpl` | `java.io.*`, `java.nio.file.*` | 5 |
| `service/external/PancakePosService` | `org.springframework.http.*`, `vn.co.cake.entity.*`, `vn.co.cake.repository.*` | 19 |
| `service/impl/AccountServiceImpl` | `org.apache.poi.ss.usermodel.*`, `vn.co.cake.request.*` | 15 |
| `service/impl/CartServiceImpl` | `java.util.*` | 6 |
| `service/impl/OrderServiceImpl` | `vn.co.cake.entity.*`, `vn.co.cake.repository.*` | 21 |
| `service/impl/PixelConversionServiceImpl` | `com.facebook.ads.sdk.serverside.*` | 8 |

- **Type sources:**
  - JDK packages from `lib/modules` (`jimage list`).
  - The others from the single jar that provides each package: `spring-web-5.3.19`, `poi-5.2.3`, `facebook-java-business-sdk-17.0.0`. There are no split packages.
  - Project packages from `target/classes`, including the QueryDSL Q-classes in `vn.co.cake.entity`.
- **Shadowing checks:** for each file, names are excluded when they are declared in the file, explicitly imported, or present in the file's own package. Names in two wildcard packages, or also in `java.lang`, are flagged; none were found. Names that occur only after a `.` are ignored (qualified names such as `vn.co.cake.dto.OrderItem`, nested refs such as `Map.Entry`).
- **Incident during the task (fixed, no impact):**
  - The first resolution dropped 3 names in `PixelConversionServiceImpl`.
  - Cause: the script stripped `//` comments before string literals, so the `//` in `"http://..."` truncated a line and left an unterminated string that swallowed the following lines.
  - The compiler caught it (`cannot find symbol`). The 6 files were restored to HEAD, the script was changed to a single-pass literal/comment tokenizer, and the files were re-resolved.
  - This bug can only drop names, which always fails compilation, never silently re-bind them. T-02a therefore stays valid: it compiled, its verbose bytecode matched, and a re-scan finds no unused imports in `entity/**`.
- **Diff:** 11 deletions, 74 insertions (import lines only).
- **VM-BYTE (verbose):** all 6 classes are identical except `LineNumberTable`. Stack-trace line numbers in these classes shift.
- **Checks:** no wildcard left in `service/**`; unused-import scan of `service/**` is clean.

## T-02c — Wildcard → explicit imports in `controller/**` (2026-11-25)

| File | Wildcards replaced | Explicit imports |
|---|---|---|
| `AM003/AM003Controller` | `org.springframework.web.bind.annotation.*` | 6 |
| `AM009/AM009Controller` | same | 4 |
| `SA001/SA001Controller` | same | 7 |
| `store/CartController` | same | 6 |
| `store/HomeController` | same + `java.util.*` | 14 |
| `external/OrderExtendController`, `ProductExtendController`, `ShopExtendController` | same | 8 each |
| `thiyen/ProductController` | same | 8 |
| `external/dto/MainOrderRequest` | `vn.co.cake.entity.*` | 5 |

- **Type sources:** `org.springframework.web.bind.annotation` comes only from `spring-web-5.3.19.jar` (28 types). `java.util` comes from the JDK modules; `vn.co.cake.entity` from `target/classes`, Q-classes included.
- **Shadowing checks:** same rules as T-02b; no conflicts and no same-package exclusions. `HomeController` keeps its explicit `javax.websocket.server.PathParam` import unchanged.
- **Diff:** 11 deletions, 74 insertions (import lines only).
- **VM-BYTE (verbose):** 11 class files (10 classes + 1 nested) are identical except `LineNumberTable`, so stack-trace line numbers shift. The annotation types on handler methods are unchanged (`RuntimeVisibleAnnotations` compared).
- **Checks:** no wildcard left in `controller/**`; no unused imports.

## T-02d — Wildcard → explicit imports in `utils`, `ai/**` (2026-11-26)

| File | Wildcard replaced | Explicit imports |
|---|---|---|
| `ai/controller/SizeAdviceController` | `org.springframework.web.bind.annotation.*` | 4 (`PostMapping`, `RequestBody`, `RequestMapping`, `RestController`) |
| `ai/service/openai/OpenAIServiceImpl` | `org.springframework.http.*` | 5 (`HttpEntity`, `HttpHeaders`, `HttpStatus`, `MediaType`, `ResponseEntity`) |
| `utils/DateUtil` | `java.util.*` | 6 (`Calendar`, `Date`, `GregorianCalendar`, `Locale`, `Objects`, `TimeZone`) |

- **Before state:** compiled from `b682363` (after the `589b5c2` merge).
- **Type sources and shadowing checks:** same lists and rules as T-02b/T-02c (`spring-web-5.3.19.jar`, JDK modules). No conflicts, no same-package exclusions. `DateUtil` imports `java.sql.Timestamp` explicitly, which doesn't clash; `Date` still binds to `java.util.Date`.
- **Diff:** 3 deletions, 15 insertions (import lines only).
- **VM-BYTE (verbose):** all 3 classes are identical except `LineNumberTable`. Stack-trace line numbers in these classes shift by +3…+5.
- **Checks:** `grep '^import (static )?….\*;'` over `src/main/java` → 0. Every import in the 3 files is used (per-name grep of the body; `unused_imports.py` was no longer in the scratchpad, so the check was done inline).

## T-03 — Delete commented-out code (partial) (2026-11-26)

`grep '^\s*//.*[;{}]\s*$'` over `src/main/java` on `d1752ab`: 60 lines in 8 files, matching audit C-03. Line numbers below are from `d1752ab`; the merge `589b5c2` moved some of them from the audit's numbers.

**Deleted (39 lines):**

| File | Lines | What |
|---|---|---|
| `controller/external/PancakeController` | 32 | `log.info` of the webhook payload |
| `security/utils/jwt/JwtUtil` | 33 | `String payload = jwt.getPayload();` |
| `service/external/OrderExtendService` | 53–56 | mail form build + `emailService.sendEmail(...)` (2 of the 4 lines don't match the grep; same comment block) |
| `service/external/PancakePosService` | 139–140, 142 | `webhookHistoryRepository.save(...)` and `log.error(...)`, plus the blank line before the `catch`. The catch block was already empty in effect. Audit's `PancakePosService:124` is this line after the merge. |
| `service/impl/AccountServiceImpl` | 231 | `account.setMailAddress(...)` in `updateAccount` |
| `service/impl/AccountServiceImpl` | 402 | `mailService.push(... MailType.CHANGE_PASSWORD)` in `changePasswordAdmin` (not the U-33 reset email) |
| `service/impl/ProductServiceImpl` | 57–84 | Pancake `createProduct` + variation mapping in `createProduct` |

**Kept (owner decision pending, plan §6 / audit C-03):**
- `payment/api/PaymentCheckoutController` 125–146: VietQR sandbox-simulate endpoint (C-11).
- `controller/AM003/AM003Controller` 524–526, 539: discount validation and `updateProductDiscountPrice` (U-38).
- `service/impl/AccountServiceImpl` 286 (285 after this task): admin forgot-password email push in `verifyMailForgetPassword` (U-33).

- **VM-BYTE (verbose):** 6 class files identical except `LineNumberTable`. Stack-trace line numbers after the removed blocks shift up.
- **Check:** the grep now finds only the 24 kept lines (3 blocks).

## T-04 — Delete `AM005Controller` (2026-11-26)

- **Removed:** `controller/AM005/AM005Controller.java` (`@Controller @Slf4j`, extends `BaseController`, no members, no mappings) and the now-empty `controller/AM005/` directory.
- **VM-GREP:** `grep -rn AM005` over `src` (Java, HTML, JS, properties, YAML, XML) and `pom.xml` before deletion → only the class itself.
- **Effect:** one fewer Spring bean; no endpoint lost. VM-CTX not run (no DB); nothing can depend on a bean with no members.
- **Noted, not changed:** `templates/AM005/AM005.html` exists but no controller returns it (no `AM005` string anywhere else). Orphan template, out of this task's scope; a candidate for a later dead-template task.
- The stale `AM005Controller.class` was deleted from `target/classes` before the gate so the suite ran without it.

## T-05 — Delete `helper/CSVHelper` (2026-11-26)

- **Removed:** `helper/CSVHelper.java`: a plain class (not a bean) with static `TYPE`, `HEADER` and `hasCSVFormat(MultipartFile)`. `helper/` still holds `EmailService` and `SmsService`.
- **VM-GREP:** `grep -rnE 'CSVHelper|hasCSVFormat'` over `src` (main, test, templates, JS, properties) and `pom.xml` before deletion → only the class itself. No file name match in resources.
- **Effect:** none at runtime; the class was never loaded. The stale `.class` was deleted from `target/classes` before the gate.

## T-06 — Remove unused local `url` in `UserLogoutSuccessHandler` (2026-11-26)

- **Change:** deleted `String url = String.format("%s%s?logout", baseUrl, RequestPathConst.SA001);` (line 40) and the blank line after it. `import vn.co.cake.common.RequestPathConst;` was used only there and became unused, so it was removed too (same file, keeps T-01's "no unused imports").
- **Why it's safe:** `String.format` has no side effects; its arguments are a field read and a compile-time constant (`RequestPathConst.SA001 = "/SA/SA001"`, inlined by javac). `url` was never read.
- **VM-BYTE (verbose, `LineNumberTable` filtered) — the only differences:**
  - `onLogoutSuccess` code: removed
    ```
    47: ldc "%s%s?logout"; iconst_2; anewarray java/lang/Object; dup; iconst_0; aload_0;
        getfield baseUrl; aastore; dup; iconst_1; ldc "/SA/SA001"; aastore;
    65: invokestatic java/lang/String.format:(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;
    68: astore 4
    ```
    The remaining instructions (`response.setStatus(HttpStatus.OK.value())`, `response.sendRedirect(baseUrl)`, `return`) are unchanged in order and targets; their offsets move from 70–92 to 47–69.
  - `stack=5, locals=5` → `stack=2, locals=4`; the LocalVariableTable entry `url` is gone.
  - Constant pool: the entries `%s%s?logout`, `vn/co/cake/common/RequestPathConst`, `/SA/SA001`, `java/lang/String`, `String.format` and `url` are gone (104 → 91 entries); the rest are renumbered, which also renumbers the `@Value`/`@Component` annotation indexes (same annotation types and values).
- **Behavior impact:** none observable (one fewer string allocation per logout).

## T-07 — Remove unreferenced Spring Data methods (2026-11-26)

| File | Removed |
|---|---|
| `repository/AccountRepository` | `findAllByAuthoritiesAndMailAddressIn` (+ now-unused `java.util.List` import) |
| `repository/CustomAccountRepository` | `findIdAllByDeletedIsFalseAndFullNameContaining` |
| `repository/impl/CustomAccountRepositoryImpl` | the QueryDSL implementation of the same method |
| `repository/CartRepository` | `findFirstCartByCartId` |
| `repository/CategoryRepository` | `findFirstByName` |
| `repository/OrderRepository` | `findByPhoneAndDeletedFalseOrderByCreatedDesc`, `findOrdersNeedingPancakeEnrichment` (native `@Query`) |
| `repository/ProvinceRepository` | `findAllByOrderByNameAsc` |
| `repository/VariationRepository` | `findFirstByPancakeProductIdAndSize` |
| `payment/repository/CheckoutPendingRepository` | `findFirstByVietqrOrderIdAndStatus` |
| `payment/repository/InventoryReservationRepository` | `findFirstByOrderIdAndVariationId`, `releaseHeldByOrderId` (`@Modifying @Query`) (+ now-unused `Optional`, `Modifying` imports) |

- **VM-GREP:** each of the 11 names grepped (`-w`) over `src` (Java, HTML, JS, XML, properties, YAML) before deletion: only the declaration (and the impl). After deletion: 0 hits. Derived/`@Query` repository methods can only be called from Java.
- **`findOrdersNeedingPancakeEnrichment` vs C-05:** `PancakePosService.enrichOrdersFromPancake(List<Order>)` takes the orders as a parameter and does **not** call this query; it has no caller either. So the query had no caller and was removed here. `enrichOrdersFromPancake` itself is untouched and stays with T-09 (Wave 2).
- **VM-BYTE (verbose, constant-pool indexes normalized):** for each of the 10 class files, the only member differences are the removed methods (with their `MethodParameters`, `Signature` and annotations), and the only constant-pool differences are entries used solely by them (names, descriptors, signatures, query strings, `Modifying`, `JPAQuery.select`). No member was added or changed; class annotations and interfaces are unchanged.
- **Unused imports:** clean in all 10 files (per-name grep).
- **VM-CTX:** UNVERIFIED at runtime (no DB). Removing methods only reduces the queries Spring Data derives/validates at startup.

## T-08 — Move `normalizeStoreUrl` into `StringUtil` (2026-11-26)

- **Before:** `helper/EmailService` and `service/mail/OrderConfirmationMailService` each had a byte-identical `private static normalizeStoreUrl(String)` and `DEFAULT_STORE_URL = "https://debase.vn"` (audit D-01, re-checked with `diff` on `e0745ee`).
- **Tests first:** 3 tests added to `utils/StringUtilTest` (blank/null default, trim + trailing slashes, missing/odd scheme incl. the case-sensitive `HTTPS://` and the lone `/` quirks). They failed to compile before the move (`cannot find symbol`). The 10 expected values were also checked by reflection against **both** original private methods on the unchanged build: 10/10 match each.
- **Change:** `public static String normalizeStoreUrl(String)` added to `StringUtil` verbatim, with `DEFAULT_STORE_URL` as a `private static final` there (only the method uses it). Both services lost their copies and call `StringUtil.normalizeStoreUrl(...)` (one call site each); `vn.co.cake.utils.StringUtil` import added. No other line changed.
- **Behavior:** identical code path; only the method's owning class changes.
- **Suite:** 323 → 326 tests (the 3 new ones), 0 failures.

## T-09 — Remove unreferenced public service methods (2026-11-26)

| File | Removed |
|---|---|
| `service/CartService` | `findOtherProductItem(Long, Long)` declaration (+ now-unused `dto.OrderItem`, `java.util.List` imports) |
| `service/impl/CartServiceImpl` | its implementation, which also held the boxed `!=` comparison bug noted in audit C-05 |
| `service/external/PancakePosService` | `@Transactional enrichOrdersFromPancake(List<Order>)` |
| `payment/service/VietQrCheckoutService` | `@Transactional getCheckoutStatus(String)` |

- **VM-GREP:** `grep -rnw` of each name over `src` (Java, templates, JS, resources) before deletion: declarations only. No reflection, `@Scheduled`, `@EventListener` or endpoint uses them. `CartServiceImpl` is the only `CartService` implementation. After deletion: 0 hits.
- **Helpers kept:** `toOrderItem`, `enrichOrderFromPancakeList`, `resolveCheckoutStatus` and `findPendingOrNull` still have other callers, so no private method became dead.
- **VM-BYTE (verbose, cp indexes normalized; script `scratchpad/member_diff.py`):** the only member differences are the removed methods plus, in `CartServiceImpl`, the synthetic `lambda$findOtherProductItem$1` and its `BootstrapMethods` entry. Every remaining member is identical; the other lambda keeps its name (`lambda$findFirstByAccountId$0`). Header method counts drop accordingly.
- **Behavior:** no executed path changes. Payment/integration classes are touched only by deleting code nothing calls.

## T-10 — Delete `PixelConversionServiceImpl` (2026-11-26)

- **Removed:** `service/impl/PixelConversionServiceImpl.java`: a `@Service` (no interface) with a single `main(List<String>, List<String>)` method that sends a hard-coded sample Facebook Conversions API event with placeholder `<ACCESS_TOKEN>` / `<ADS_PIXEL_ID>` and `printStackTrace`.
- **VM-GREP:** `grep -rli pixelconversion` over `src` and `pom.xml` before deletion → only the file itself (no injection point, no test, no config key). The other `facebook` hits in `src/main/resources` are icon fonts/CSS and social links, unrelated.
- **Effect:** one fewer bean; nothing injected it. The `facebook-java-business-sdk` dependency in `pom.xml` is now unused; removing it is a separate dependency change and was **not** done.
- **VM-CTX:** UNVERIFIED (no DB). Nothing can fail to wire, since no class referenced the bean.
- The stale `.class` was deleted from `target/classes` before the gate.

## T-11 — Remove the empty `CustomCartRepository` fragment (2026-11-26)

- **Removed:** `repository/CustomCartRepository.java` (empty interface) and `repository/impl/CustomCartRepositoryImpl.java` (`@Repository`, extends `BaseRepository`, only a constructor storing the `EntityManager`; no methods).
- **Changed:** `repository/CartRepository`: `extends JpaRepository<Cart, Long>, CustomCartRepository` → `extends JpaRepository<Cart, Long>`. `javap -p` after: the interface has only `findFirstCartByAccountId(Long)` plus the inherited `JpaRepository` API, so every method callers use is unchanged.
- **VM-GREP:** `CustomCartRepository` / `customCartRepository` over `src` before deletion → only these three files. After: 0.
- **Side effects:** `BaseRepository` has no state or constructor logic. Removing the fragment means Spring Data no longer composes it into the `CartRepository` proxy, and one `@Repository` bean (`customCartRepositoryImpl`) is gone; it contributed no methods.
- **VM-CTX:** UNVERIFIED (no DB).
- Stale `.class` files deleted from `target/classes` before the gate.

## T-00 — BLOCKED (2026-11-24)

**What happened.** `.git/hooks/pre-commit` (portable-skills pack) only lets an agent commit a tree that its `gate.sh` has stamped as passing. I ran the gate on the current tree:

```
LINT=skipped  TYPECHECK=skipped
gate: test: mvn -q test
TEST=fail   (sh: mvn: command not found)
SKIPS=pass  SECRETS=pass  DEPS=pass
GATE_DEFINITIONS_CHANGED=pom.xml   (gate diffs against merge-base with develop; the 71 unmerged feature commits include pom.xml — not changed in this refactor)
GATE_RESULT=fail
```

**Why it can't pass as things stand:**
1. `mvn` is not on PATH, and the gate runs a bare `mvn -q test`.
2. Java TLS to Maven Central fails under Avast HTTPS scanning unless `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` is set.
3. Even with 1 and 2 fixed, the suite has the known pre-existing failure **BF-01**, so `TEST=fail`.

The hook only accepts overrides (`AGENT_TEST_CMD`, gate-mode) from the **user's own** environment. The agent must not set them inline or edit the gate's switches. The block is an environment/policy issue, not a defect in the staged changes (those are docs + new tests only).

**Options (owner's choice):**

| Option | What the owner does | Effect |
|---|---|---|
| A | Decide BF-01 (approve updating the stale expectation to `Check Sản Phẩm`, a test-only change), **and** put Maven on PATH with `MAVEN_OPTS` set in the user environment | Gate can pass honestly. The agent then commits T-00 and every Wave 1 task. |
| B | Commit T-00 yourself from your own terminal (the hook only acts on agent commits) | Unblocks quickly. Later per-task commits hit the same block, so either repeat B per task or move to A. |
| C | The agent executes Wave 1 tasks **uncommitted**, one at a time, verifying each (VM-BYTE + suite) and recording it here | No commits. T-01 and T-02 touch the same files, so per-task commits can't be separated afterwards. |
| D | Set your own gate override (e.g. `AGENT_TEST_CMD` with the full Maven path, `MAVEN_OPTS`, excluding BF-01) or gate-mode `off` in your environment | Your policy decision. The agent will not set it. |

**Staging state (unchanged by the agent):** 31 files staged (docs 00–13 + 17 tests) by an external auto-add, plus `.idea/workspace.xml` modified and unstaged. Before any commit, the staged set must be exactly `refactor-docs/**` + the 17 new test files.
