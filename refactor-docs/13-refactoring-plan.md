# 13 — Refactoring Plan

Baseline `a0515d8`. Source of findings: `12-refactoring-audit.md`. **No task is approved yet.** Each task below needs explicit owner approval before Phase 6 executes it, and only one task runs at a time.

## 1. Rules for every task (Phase 6)

1. **Pre-check.** The working tree contains only the expected changes. The full suite is at baseline: **297 run, 0 failures** (BF-01 fixed in `f132489`). `gate.sh` must pass before each commit; this is enforced by the repo's agent pre-commit hook.
2. **Bytecode snapshot (VM-BYTE).** Before editing, compile and dump `javap -c -p -constants` for every class in the task's file list into `scratchpad/<task>/before/`.
3. **Change.** Make the smallest edit. Touch only the listed files. Follow `docs/coding_rule.md`.
4. **Review the diff.** Read the full `git diff` for the task. Nothing outside the file list may change.
5. **Compile.** Then dump to `after/` and diff against `before/` for VM-BYTE tasks. The expected result is stated per task.
6. **Run the full suite.** It must still show 297 run with only BF-01 failing, plus any tests the task adds.
7. **Record.** Update `14-refactoring-progress.md` and `00-refactor-state.md`.
8. **Commit.** One commit per task, made only with owner approval. Conventional message `refactor(<area>): …`, no agent trailer.
9. **Stop** before the next task.

If anything unexpected happens (a bytecode difference that wasn't expected, a test change, a new failure, a file outside scope), the task stops, is marked BLOCKED, and the issue is reported.

**Environment (from `10` §3):**

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-11.0.5"; $env:MAVEN_OPTS = "-Djavax.net.ssl.trustStoreType=Windows-ROOT"; & "$env:USERPROFILE\.m2\wrapper\dists\apache-maven-3.9.9-bin\33b4b2b4\apache-maven-3.9.9\bin\mvn.cmd" -B test
```

## 2. Wave 0: commit the baseline (prerequisite, owner action)

| ID | Task | Files | Notes |
|---|---|---|---|
| T-00 | Commit `refactor-docs/**` and the 17 Tier A test files as the documented baseline | docs + `src/test/java/**` new files only | **Exclude** `.idea/workspace.xml`. Gives Phase 6 a clean before-point and makes per-task diffs reviewable. Needs a commit approval. |

## 3. Wave 1: LOW risk, mechanically verifiable (recommended)

| ID | Finding | Change | Files | Expected VM-BYTE | Tests | Risk |
|---|---|---|---|---|---|---|
| T-01 | A-01 | Remove 14 unused imports | 10 files (listed in 12 A-01) | **Identical** | none new | LOW |
| T-02a | A-02 | Wildcard → explicit imports, `entity/**` | 15 files | **Identical** | none | LOW |
| T-02b | A-02 | Same, `service/**` | 6 files | **Identical** | none | LOW |
| T-02c | A-02 | Same, `controller/**` | 10 files | **Identical** | none | LOW |
| T-02d | A-02 | Same, `utils`, `ai/**` | 3 files | **Identical** | none | LOW |
| T-03 | C-03 (partial) | Delete commented-out code **except** the VietQR sandbox endpoint, the AM003 discount-apply block and the admin reset-email push | `ProductServiceImpl` and others found by the grep (final list fixed at execution and shown before editing) | **Identical** | none | LOW |
| T-04 | C-01 | Delete empty `AM005Controller` | 1 file (and its package dir) | n/a (class removed) | none | LOW |
| T-05 | C-02 | Delete unreferenced `helper/CSVHelper` | 1 file | n/a | none | LOW |
| T-06 | B-01 | Remove unused local `url` | `security/user/UserLogoutSuccessHandler.java` | Differs only by the removed `String.format` sequence | none | LOW |
| T-07 | C-04 | Remove 11 unreferenced repository methods, plus the matching `CustomAccountRepositoryImpl` method | `AccountRepository`, `CustomAccountRepository(+Impl)`, `CartRepository`, `CategoryRepository`, `OrderRepository`, `ProvinceRepository`, `VariationRepository`, `CheckoutPendingRepository`, `InventoryReservationRepository` | Removed methods only | none | LOW (VM-CTX unverified) |

T-02 file counts were verified by grep: 15 + 6 + 10 + 3 = 34 files. Wildcards in `entity/thiyen` and `entity/external` belong to T-02a; those in `controller/external/dto`, `controller/thiyen`, `SA001`, `AM003` and `AM009` belong to T-02c.

**Wave 1 outcome:** about 50 files cleaned, zero behavior change, proven by identical bytecode for T-01, T-02 and T-03.

## 4. Wave 2: LOW–MEDIUM, needs the specific approvals noted

| ID | Finding | Change | Extra approval gate | Tests | Risk |
|---|---|---|---|---|---|
| T-08 | D-01 | Move the identical `normalizeStoreUrl` and `DEFAULT_STORE_URL` into `utils/StringUtil` as public static; both mail services call it | Shared component (gate 4) | **Add** characterization tests for the method **before** moving it, by copying the current behavior | LOW |
| T-09 | C-05 | Remove unreferenced public methods `CartService.findOtherProductItem` (interface + impl), `PancakePosService.enrichOrdersFromPancake`, `VietQrCheckoutService.getCheckoutStatus` | Integration / payment code (gate 7) | none | LOW–MEDIUM |
| T-10 | C-06 | Delete `PixelConversionServiceImpl` | Bean removal | none | MEDIUM (VM-CTX unverified) |
| T-11 | C-07 | Remove the empty `CustomCartRepository` fragment and drop it from `CartRepository extends …` | Repository wiring | none | MEDIUM (VM-CTX unverified) |
| T-12 | B-02 | Remove the write-only `authentication` field in `UserContextRepository` | Security component | none | LOW |
| T-13 | H-02 | Rename `storesSecurityContextInRedis` → `storeSecurityContext` | Security component | none | LOW–MEDIUM |
| T-14 | C-10 | Delete unreferenced template `SA001/SA001.html` | — | none | LOW–MEDIUM |

## 5. Wave 3: MEDIUM, requires Tier B tests first

| ID | Finding | Prerequisite tests | Change |
|---|---|---|---|
| T-15 | D-02 | New `AM003ControllerUploadTest` (mocked `S3Service`/`ProductService`): call order file…file9, URL set on each setter, skipping null/empty files, exception propagation | Private upload helper inside `AM003Controller` |
| T-16 | D-03 | New `OpenAIServiceImplTest` (mocked `RestTemplate`): every catch branch for both methods, plus messages and types | Shared private POST + error-mapping method |
| T-17 | I-01 | SA001 OTP controller test; `CartServiceImpl.create` test | Remove unreachable branches |
| T-18 | H-01 (optional) | Existing `S3ServiceImplTest` | Rename `S3Service`/`S3ServiceImpl` → `ImageStorageService`/`LocalImageStorageService` (bean name changes) |

## 6. Needs an owner decision (not schedulable yet)

| Item | Question |
|---|---|
| C-09 `JwtUtil` | Remove it? It's unused, has a hardcoded secret, and carries a `@PropertySource` side effect. If yes: run the task together with a Tier C context-start check. |
| C-11 + sandbox block | Keep or remove the VietQR sandbox-simulate endpoint (commented) and `runSandboxPaymentConfirm`? |
| C-08 `WebhookHistoryRepository` | Keep it, for a future fix of U-28? |
| L-02 | Replace `printStackTrace`/`System.out` with SLF4J? This changes output destination and format. |
| BF-01 | Which page title is correct? |
| U-items (M-01) | Triage: intended or bug → separate tickets |

## 7. Explicitly NOT planned (and why)

| Item | Reason |
|---|---|
| D-04 payment and checkout de-duplication | CRITICAL. Transactions, locks, idempotency. Requires Tier B + Tier C and a separate proposal. |
| D-05 truncate helpers | Not equivalent; merging changes outputs. |
| D-06 DTO merges | JSON contract risk. |
| F-01 splitting large classes | Moves `@Transactional`/`@Async` boundaries. |
| G-01 removing service interfaces | Project convention. |
| H-03 config key renames | Config change in every environment. |
| H-04 `OrderItem` rename | High churn, shadowing risk. |
| H-05 job renames | Low value. |
| I-02 `ThreadPoolManager` | Trap: the first assignment enforces a required property. |
| J-01 `ObjectMapper` consolidation | Low benefit; injecting the Spring bean would change deserialization (U-28). |
| K-01 performance | Changes query semantics and side effects. |
| L-01 security and architecture | Behavior changes. Recommended as prioritized tickets: U-34 and U-32 (account takeover) first, then U-36/U-24 (unauthenticated writes) and U-30/U-31/U-35 (secrets in logs and email). |
| Entity mapping cleanups (e.g. `Product.java:70` `@JoinColumn` on String, generator names) | `ddl-auto=update` would alter the production schema. |

## 8. Proposed approval for Phase 6

Recommended first approval: **T-00** (baseline commit), then **Wave 1 (T-01 … T-07)**, executed one task at a time with a stop and report after each. Wave 2 tasks are approved individually afterwards. Wave 3 tasks start only after their prerequisite tests are approved and written.
