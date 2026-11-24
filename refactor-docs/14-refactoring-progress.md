# 14 — Refactoring Progress

Approved by owner 2026-11-24: **T-00 + Wave 1 (T-01 … T-07)**, then option A to unblock T-00. Regression baseline: **297 run, 0 failures**.

| Task | Status | Notes |
|---|---|---|
| BF-01 fix (option A) | **COMPLETED** | `f132489`; gate passed (53 tests) |
| T-00 Commit baseline docs + Tier A tests | **COMPLETED** | Committed in the commit that contains this line (`docs: add refactor baseline …`); gate passed. Was BLOCKED before option A (see below). |
| T-01 Remove unused imports | PENDING | Waits on T-00 (per-task commits need a clean base) |
| T-02a–d Wildcard → explicit imports | PENDING | |
| T-03 Delete commented-out code (partial) | PENDING | |
| T-04 Delete `AM005Controller` | PENDING | |
| T-05 Delete `CSVHelper` | PENDING | |
| T-06 Remove unused local in `UserLogoutSuccessHandler` | PENDING | |
| T-07 Remove unreferenced repository methods | PENDING | |

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
