# 14 — Refactoring Progress

Approved by owner 2026-11-24: **T-00 + Wave 1 (T-01 … T-07)**, then option A to unblock T-00. Regression baseline: **297 run, 0 failures**.

| Task | Status | Notes |
|---|---|---|
| BF-01 fix (option A) | **COMPLETED** | `f132489`; gate passed (53 tests) |
| T-00 Commit baseline docs + Tier A tests | **COMPLETED** | Committed in the commit that contains this line (`docs: add refactor baseline …`); gate passed. Was BLOCKED before option A (see below). |
| T-01 Remove unused imports | **COMPLETED** | 14 import lines in 10 files. VM-BYTE identical (11 class files). Gate passed. Committed in the commit containing this line. |
| T-02a Wildcard → explicit imports, `entity/**` | **COMPLETED** | 15 files: `javax.persistence.*` replaced by 101 explicit imports. Verbose VM-BYTE identical apart from line numbers. Gate passed. Committed in the commit containing this line. |
| T-02b–d Wildcard → explicit imports (`service`, `controller`, other) | PENDING | |
| T-03 Delete commented-out code (partial) | PENDING | |
| T-04 Delete `AM005Controller` | PENDING | |
| T-05 Delete `CSVHelper` | PENDING | |
| T-06 Remove unused local in `UserLogoutSuccessHandler` | PENDING | |
| T-07 Remove unreferenced repository methods | PENDING | |

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
