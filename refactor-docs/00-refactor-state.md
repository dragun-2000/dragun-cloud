# 00 — Refactor State

> Persistent checkpoint. Read this first in every new session, after `CLAUDE.md` (none exists in this repo).

| Item | Value |
|---|---|
| Last updated | 2026-11-26 |
| Current phase | **Phase 6 — in progress** (approved: T-00 + Wave 1 + option A, 2026-11-24). See `14-refactoring-progress.md`. |
| Completed phases | Phase 1, Phase 2, Phase 3 (docs 06–09), Phase 4 (docs 10–11 + Tier A tests), Phase 5 (docs 12–13) |
| Current approved scope | Documentation (`refactor-docs/**`) + **Tier A characterization tests** (new files under `src/test/java/**` only; approved by owner 2026-11-24). **No production source, existing-test, config or dependency changes are approved.** |
| Current task | None. Completed: BF-01 (`f132489`), T-00 (`515dc24`), T-01 (`3555a3f`), T-02a (`67b9e58`), T-02b (`4c08f4f`), T-02c (code in `ab72804`, docs `b682363`), T-02d (`d1752ab`), T-03 (next commit). Branch then merged `dev/compression-image` (`589b5c2`, owner, 2026-11-26). |
| Next permitted action | T-04 (`AM005Controller`), then T-05…T-07 one at a time. |
| Git housekeeping (2026-11-24, owner-approved) | `.claude/worktrees` unstaged; `.claude/worktrees/` added to **local** `.git/info/exclude` (not `.gitignore`); 4 agent worktrees removed and 4 `worktree-agent-*` branches deleted (no unique commits; all 17 test files verified byte-identical in this checkout first). Note: new files keep getting auto-staged by something outside the agent (likely the IDE). |

## Git baseline

| Item | Value |
|---|---|
| Working branch | `dev/refactor_20261123` (local only, no upstream) |
| Created from | `dev/compression-image` @ `a0515d8` ("add cancel order"), reflog: "Created from HEAD" |
| **Refactor baseline commit** | `a0515d8dc808dd410123c565c2544e5ee99daff4` |
| Last verified Git commit | `a0515d8` — working tree clean, no stash, no uncommitted user changes (verified 2026-11-23) |
| Repo default/base branch | `develop` (`origin/HEAD -> origin/develop`) |
| Relation to base | Branch is **71 commits ahead** of `develop` (`2a48214`); `origin/develop` (`eb020b6`) has **2 commits not in this branch** (`4994c09 config static file`, `eb020b6 update home`). Local `develop` is behind `origin/develop`. |
| Untracked / ignored | Only `target/` (ignored). `.env` is gitignored and not present. |

**Baseline rule:** all refactoring diffs and regression comparisons are measured against `a0515d8`, **not** against `develop` — the 71 unmerged feature commits (VietQR, VNPay, image compression, cancel order) are part of the baseline behavior.

## Last test execution result

2026-11-24 on `a0515d8`, JDK 11.0.5, Maven 3.9.9, `mvn -B -U test` with `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT` (Avast HTTPS interception breaks Java TLS otherwise):
- Compile: PASS (342 main incl. 34 QueryDSL Q-classes, 10 test).
- Tests: **53 run, 52 pass, 1 fail, 0 error, 0 skipped.**
- Expected baseline failure **BF-01** `ImageCompressionTemplateTest.pageRenders_withRowsAndEmptyState` — stale expectation after commit `077f1eb` renamed the page title. Not to be "fixed" by the refactor.
Details: `10-baseline-results.md`.

**Current regression baseline (after Tier A, 2026-11-24):** same command → **297 run, 296 pass, 1 fail (BF-01), 0 errors** — confirmed on two consecutive runs. `10-baseline-results.md` §6.

**Updated baseline (after BF-01 fix `f132489`):** **297 run, 0 failures.** **After merge `589b5c2` (2026-11-26): 323 run, 0 failures, 0 errors** (`mvn -q -o test`, counts from `target/surefire-reports`). In this agent session the commands need `PATH=/d/freeland/apache-maven-3.9.9/bin:$PATH`, `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`, `JAVA_HOME=C:\Program Files\Java\jdk-11.0.5` exported. New terminals have them from the user environment.

## Tasks

| Status | Tasks |
|---|---|
| PENDING | Phase 6 tasks T-00…T-18 (none approved); Phases 7–10; Tier B/C tests (proposed) |
| IN_PROGRESS | — |
| COMPLETED | Phase 1, Phase 2, Phase 3, Phase 4, Tier A characterization tests, Phase 5 |
| BLOCKED | — |
| REJECTED | — |

## Known risks (summary — details in 01–05)

See `01-system-overview.md` §Risks. Top items: committed literal secrets in properties; VNPay hash secret logged at INFO; plaintext OTP logged; Pancake failure dumps containing the API token are emailed to a hardcoded address; no security filter chain covers `/`, `/cart*`, `/payment*`, `/api/**`; `ddl-auto=update` in production; `@Async` likely runs on an unbounded `SimpleAsyncTaskExecutor`; near-zero test coverage of order/payment flows.

**Phase 3 additions** (register in `09-side-effects.md` §2, U-01…U-44). New CRITICAL security findings in existing behavior: U-32 (`/SA/SA001/register` with `accountId` edits any account, unauthenticated), U-34 (`/SA/SA002/change-password` lets any customer reset any account's password by id), U-36 (unauthenticated write APIs). Refactor traps: checked `CommonServletException` commits partial writes (U-01); ineffective private `@Transactional` (U-27); Job004 cancels VNPay orders (U-19). One scout claim refuted (committed OpenAI key — value is empty).

## Unresolved questions for the owner

1. Are `/api/extend/**`, `/api/thiyen/**`, `/api/v2/pancake/**`, `/setup/location` called by external systems (access logs / integration owners)? Determines whether they can ever be considered dead.
2. Is there a gateway/IP allow-list in front of the public webhook and `/api/**` endpoints?
3. Should security/data-integrity findings (secret logging, OTP logging, unguarded endpoints) be handled as separate bug-fix tickets outside this refactor? (Recommended: yes — this refactor must not change behavior.)
4. Is a local PostgreSQL (docker-compose `debase-db`) available for Phase 4 baseline/integration tests, or unit-level only?
5. Target refactor scope: whole codebase, or a priority subset (e.g. payment + order modules)?
6. U-32 / U-34 (account takeover paths) are exploitable today — should they be raised as urgent bug tickets immediately, independent of this refactor?
7. Is the voucher-not-applied-to-order-total behavior (U-02) intended? (Affects what characterization tests assert.)
8. BF-01: is `Check Sản Phẩm` the intended page title (then the test is stale) or should it be `Nén Ảnh Sản Phẩm`?
9. Approve Tier A characterization tests (~90 pure unit tests, no prod change, no new dependency)? Tier B per module? Tier C via docker-compose DB or Testcontainers?
