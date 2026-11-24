# 10 — Baseline Build & Test Results

| Item | Value |
|---|---|
| Baseline commit | `a0515d8` (`dev/refactor_20261123`), working tree clean except `.idea/workspace.xml` (IDE) and `refactor-docs/**` |
| Run date | 2026-11-24 |
| JDK | Oracle JDK **11.0.5** (`C:\Program Files\Java\jdk-11.0.5`) — matches production `eclipse-temurin:11` major version |
| Maven | 3.9.9 (cached wrapper distribution under `%USERPROFILE%\.m2\wrapper\dists`) |
| Command | `mvn -B -U test` with `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (see §3) — **no `clean`** (keeps existing `target/*.jar`) |
| Duration | 31 s |
| Raw log | `C:\Users\Dragun\AppData\Local\Temp\claude\9d8387723b1a6e83\f31c55ef-7f51-455f-aadd-8815ce959f60\scratchpad\phase4-baseline-test-final.log` (scratchpad, not durable) + `target/surefire-reports/` |
| External effects | None. All tests are plain JUnit 5 + Mockito; no Spring context, no DB, no network. |

## 1. Compilation

| Step | Result |
|---|---|
| QueryDSL APT + `compile` | **PASS** — 342 sources (308 hand-written + generated Q-classes) |
| `testCompile` | **PASS** — 10 sources |
| javac notes | `repository/BaseRepository.java` uses unchecked operations (1 note) |
| Maven model warnings | Duplicate dependency `com.fasterxml.jackson.core:jackson-core` (2.9.8 vs managed) in `pom.xml`; malformed POMs of transitive `javassist 3.18.2-GA / 3.20.0-GA` |

## 2. Tests

**Totals: 53 run, 52 passed, 1 failed, 0 errors, 0 skipped.**

| Test class | Run | Result |
|---|---|---|
| `controller.system.ImageCompressionTemplateTest` | 1 | **FAIL** (pre-existing, see below) |
| `enums.PancakeFulfillmentStatusTest` | 7 | PASS |
| `service.aws.impl.S3ServiceImplTest` | 2 | PASS |
| `service.external.PancakeOrderStatusTest` | 1 | PASS |
| `service.image.ImageCompressorTest` | 11 | PASS |
| `service.image.ProductImageCompressionServiceTest` | 10 | PASS |
| `vnpay.service.VnpayIpnServiceTest` | 10 | PASS |
| `vnpay.support.VnpayFormatsTest` | 4 | PASS |
| `vnpay.support.VnpaySignerTest` | 7 | PASS |

### Baseline failure BF-01 — `ImageCompressionTemplateTest.pageRenders_withRowsAndEmptyState:36`

- Assertion: rendered page must contain `"Nén Ảnh Sản Phẩm"` ("trang nằm trong layout admin, có mục menu").
- Cause (verified): commit `077f1eb` ("Compression images", 2026-11-16) changed the page title in `templates/system/image-compression.html` from `Nén Ảnh Sản Phẩm` to `Check Sản Phẩm`; the sidebar label (from `7a60e1b`) is also `Check Sản Phẩm`. The test was not updated. The string no longer exists anywhere under `src/main/resources`.
- Classification: **pre-existing, deterministic, stale test expectation** — not caused by this environment (the preceding assertions with Vietnamese diacritics pass, so encoding is fine).
- Action: **none.** Per refactor rules the expectation is not changed. Any regression comparison must treat this test as *expected-fail* at baseline. Fixing it (either side) needs an owner decision on which label is correct.

## 3. Environment issues found (not project defects)

| Issue | Evidence | Workaround used |
|---|---|---|
| `mvn` not on PATH; no `mvnw` in repo | — | Full path to cached Maven 3.9.9 |
| TLS to Maven Central fails from Java with PKIX error | Avast Web/Mail Shield intercepts HTTPS (issuer `CN=Avast Web/Mail Shield Root`); trusted by Windows, not by any JDK cacerts (also failed with Corretto 11.0.17) | Per-run `-Djavax.net.ssl.trustStoreType=Windows-ROOT` (Java reads the Windows trust store). No system, JDK or project setting changed. |
| Failed download cached as `surefire-junit-platform-2.22.2.pom.lastUpdated` | `~/.m2` | `-U` to force re-resolution |

**Reproduce the baseline (PowerShell):**
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11.0.5"; $env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"; & "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9-bin\33b4b2b4\apache-maven-3.9.9\bin\mvn.cmd" -B test
```

## 4. Not executed (and why)

| Item | Status | Reason |
|---|---|---|
| Application startup / Spring context | NOT RUN | Requires PostgreSQL; `ddl-auto=update` would mutate whatever DB it connects to; `LoadDatabase` seeds an admin. Needs an approved disposable DB. |
| Integration / repository / controller tests | NONE EXIST | — |
| Code coverage | NOT MEASURED | No JaCoCo in `pom.xml`. Can be measured without editing the pom (`mvn org.jacoco:jacoco-maven-plugin:<ver>:prepare-agent test …:report`) — this downloads a new tool, so it awaits approval. |
| Static analysis (Checkstyle/PMD/SpotBugs/Sonar) | NOT RUN | None configured |
| Packaging (`package`) | NOT RUN | Not needed for baseline; Docker/Jenkins build uses `-DskipTests` |

## 5. Baseline verdict

- **Build: PASS.**
- **Unit tests: 52/53 PASS; 1 known pre-existing failure (BF-01).**
- **Behavioral baseline for business flows: UNVERIFIED** — the existing suite covers only image compression, Pancake status mapping, VNPay signing/formatting and VNPay IPN (with mocked collaborators). All other flows in `06-current-business-flow.md` have no executable baseline (see `11-test-coverage-gaps.md`).

## 6. Baseline after Tier A characterization tests (2026-11-24)

Owner approved Tier A. 17 new test files (244 test runs, ~2,640 lines) were added under `src/test/java/**`. **No production, existing-test, pom or config file was changed** (verified: each agent worktree showed only untracked new files; no path collided with an existing file).

| Run | Command | Result |
|---|---|---|
| Full suite #1 | `mvn -B test` (same env as §3) | **297 run, 296 pass, 1 fail (BF-01), 0 errors, 0 skipped** |
| Full suite #2 (stability) | same | identical — 297 / 1 fail (BF-01) |

**This is the regression baseline for Phase 6+:** every refactor task must keep `297 run / only BF-01 failing` (plus any further approved tests).

| New test class | Runs | Pins |
|---|---|---|
| `utils.OrderPricingUtilTest` | 13 | BR-P1, per-line `intValue()` discount truncation |
| `entity.ProductPricingTest` | 11 | BR-P7, create/update vs Pancake-sync discount differences |
| `service.impl.OrderServiceImplPricingTest` | 9 | BR-P2/P3/P4, null shipping fee NPE |
| `payment.support.PaymentOrderTotalCalculatorTest` | 9 | BR-P6 (FLOOR, threshold before discount, no cap) |
| `payment.util.VietQrContentNormalizerCharacterizationTest` | 11 | content normalization, truncation without upper-case |
| `payment.service.VietQrWebhookServiceAmountCharacterizationTest` | 3 | BR-P10 (package-private static helpers) |
| `vnpay.support.VnpayFormatsCharacterizationTest` | 14 | gaps not in existing `VnpayFormatsTest` |
| `controller.external.dto.MainOrderRequestCharacterizationTest` | 12 | BR-P12 field mapping + **actual JSON keys** + exception types |
| `utils.PhoneValidatorTest` | 42 | BR-A1 |
| `service.OtpVerificationServiceTest` | 19 | BR-A4 (expiry via negative minutes; window reset not testable — no clock injection) |
| `handler.GlobalExceptionHandlerTest` | 23 | 08 §2 contract |
| `ai.service.impl.SizeAdviceServiceImplTest` | 50 | BR-C11, availability fix-ups, chat parsing |
| `utils.BigDecimalUtilTest` / `StringUtilTest` / `FunctionUtilTest` / `PageUtilTest` / `DateUtilTest` | 6 / 7 / 3 / 3 / 9 | utility outputs |

Test-hygiene notes: `BigDecimalUtilTest` and `DateUtilTest` set and restore the default `Locale`/`TimeZone`; `OtpVerificationServiceTest` sets `@Value` fields via `ReflectionTestUtils` (config only). Tests construct services through their constructors — a constructor-signature refactor will require updating them (that is a test-only change and must be reported). No wildcard imports, no Spring context, no network, no sleeps (verified by grep).

## 7. BF-01 resolved and environment fixed (2026-11-24, owner option A)

- **BF-01:** the owner confirmed `Check Sản Phẩm` is the intended title. The stale expectation in `ImageCompressionTemplateTest:36` was updated (one line, test only) and committed as **`f132489`** after `gate.sh` passed (`TEST=pass`, 53 tests).
- **Environment (user scope, persistent):**
  - The user `PATH` already pointed to `D:\freeland\apache-maven-3.9.9\bin`, but the folder was missing. The cached Maven 3.9.9 distribution was copied there, and no PATH edit was needed.
  - User variable `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` was set.
  - `JAVA_HOME` (user) was already `jdk-11.0.5`.
  - New terminals pick these up. Sessions started before the change must export them.
- **New regression baseline:** **297 run, 0 failures, 0 errors.** Every task must keep this exactly (plus any approved new tests).

**Process note:** the agent worktrees were initially created from `eb020b6` (`origin/develop`), not the baseline; each agent switched its own worktree to `a0515d8` before writing tests (verified: all four at `a0515d8`).
