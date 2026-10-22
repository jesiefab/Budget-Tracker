# tasks.md — Implementation Steps

> **Phase 3 of 3.** A checked `- [x]` box is the **only authorization to touch source code** for that task.
> Each task names the file(s) it may touch and a "done when" check; the harness (`tests/run-tests.sh`) must exit 0 after every task.
> Unplanned discoveries must first go through the change-request log at the bottom, then the phase gate ([agent-rules.md](agent-rules.md)).

**references:** [requirements.md](requirements.md) · [design.md](design.md) · [agent-rules.md](agent-rules.md)

---

## Current work — persistence hardening release (Phase-3 continuing)

- [x] **T1 — Commit the already-ported persistence fixes.** *R8.2, R8.3, R8.4, R8.5, R8.6, R8.7, R8.9, R8.10, R2.3, R4.6*
  Files: `Desktop/SaveManager.java`, `Desktop/DataStore.java`, `Desktop/BudgetPanel.java`, `tests/*`
  Done when: working tree diff = only the ported fix surface; harness green; user asked-and-approved commit exists.
  Note: these fixes are **already implemented and verified (24/24 harness)** — this task is the commit + release step only.

- [x] **T2 — Cut the corrected release (`v3.0.1`).** *R8.x, R2.3, R4.6*
  Files: `dist/` artifacts, GitHub release, `README.md` download table, `Desktop/META-INF/MANIFEST.MF`
  Done when: `build-exe.bat` produces installer + zip from the fixed code; release assets replaced/published; README download table updated.
  Note: GitHub `v3.0.0` assets still ship pre-fix binaries — **requires explicit user authorization**.

- [x] **T3 — Exercise `run-tests.bat` once on Windows.** *R8 harness coverage*
  Files: none (verification only)
  Done when: `.bat` path executed, exit 0 captured. Previously only the `.sh` path was run.

- [x] **T4 — Retire the twin core.** *R8, R4.6, R2.3 (removes duplicate truth)*
  Files: `src/main/java/`, `tests/Tests.java` (twin suite), `tests/run-tests.*`, README project-structure section
  Done when: `src/main/java` deleted or formally deprecated; `Tests.java` runs Desktop checks only (12 checks — expectation count updated, not weakened); harness green; README updated.
  Gate: needs user decision first — deprecate vs delete vs promote-twin-to-primary.
  Resolution (user-delegated decision, 2026-10-22): **delete** — the twin was never packaged, its XP curve contradicted shipped balance (900-XP linear vs shipped 4500-XP quadratic), and its semantics re-land tests-first in app/ (T10/T11). README needed no edit (it never listed the tree).

## Next work — spec check automation

- [x] **T5 — Make `scripts/check-spec.sh` executable part of convention.** *R-all (spec hygiene)*
  Files: `scripts/check-spec.sh`
  Done when: script has exec bit, runs on Git Bash/WSL/Linux, catches a deliberately-broken spec in dry-run, and its PASS output is shown in agent onboarding.
  Evidence (2026-10-22): git mode 100755; negative dry-run (bogus citation appended to design.md) → `FAIL: IDs in design.md missing from requirements.md`, exit 1; restored → exit 0; README onboarding bullet and agent-rules commit rule now reference it as a hard gate.

## Framework rebuild — approved stack pivot (gated, not started)

- [x] **T9 — Scaffold the new Maven project `app/` + bootstrap adapter.** *R-all (structural groundwork)*
  Files: `app/pom.xml` (Spring Boot 4.1.x parent, OpenJFX 25 deps via `javafx-maven-plugin`, spring-boot-starter-test), `app/src/main/java/…/BudgetTrackerApp.java`
  Done when: `mvn -pl app javafx:run` opens an empty window; Spring context boots with `WebApplicationType.NONE` via `init()` → `SpringApplicationBuilder`; `stop()` closes the context cleanly; `mvn test` green on a boot smoke test. Follow design.md § Bootstrap exactly (research-backed, avoids the known loader/product pitfalls).

- [ ] **T10 — Persistence slice before any UI.** *R8.1–R8.11*
  Files: `app/src/main/resources/db/migration/V1__schema.sql` (Flyway), Spring `JdbcTemplate` repositories, DTOs
  Done when: repositories tested against **real temp SQLite files** with the production migrations (not H2); corruption/missing-values/escaping rules from R8 have equivalent tests; user DB lives in the OS app-data directory (never the install dir).

- [ ] **T11 — Port domain math tests-first.** *R2.1–R2.7, R3.1–R3.6, R4.1–R4.6*
  Files: `BudgetService`, `XpService`, `SavingsService` and test classes
  Done when: every XP/proration edge case currently in `tests/DesktopTests.java` (real month lengths, XP-derived level, stale `LEVEL=` line, quadratic 4500-XP rank-10 curve) reproduces exactly against the ported services before any UI consumes them.

- [ ] **T12 — JavaFX presentation layer.** *R1.1–R1.6, R2.7, R5.1–R5.3, R6.1–R6.3, R7.1–R7.3*
  Files: `app/src/main/resources/fxml/*.fxml`, CSS per theme, FXML controllers
  Done when: FXML views for Expenses/Budget/Savings/History/Settings all render against the real services; `loader.setControllerFactory(context::getBean)` used everywhere; stateful controllers are **prototype-scoped** (not singleton); all 5 themes are JavaFX CSS switchable live; each requirement acceptance criterion from requirements.md is demonstrable in the built UI.

- [ ] **T13 — Legacy import + Windows packaging.** *R8.11 + release infrastructure*
  Files: import parser (reads v3 plaintext saves incl. escapes), `jpackage` config, WiX Installer
  Done when: import tested against a real v3 save and a v2 save; file renamed not deleted; re-import doesn't duplicate; jpackage app-image built with the middle-ground module shape from design.md § Packaging (JavaFX modules on the module path, launcher not extending `javafx.application.Application`); installer verified on a clean Windows VM/machine.

## Later work — feature backlog (one small spec per item, see spec-workflow.md)

- [ ] **T6 — Spec: recurring/subscription expenses.** *R1, R2* — needs its own `specs/recurring-expenses/{requirements,design,tasks}.md` before any code.
- [ ] **T7 — Spec: CSV export of expense history.** *R5* — same gating; no code before its spec.
- [ ] **T8 — Spec: Android parity audit** — table desktop features vs Android implementation, gaps filed as their own small specs. *R1–R8 (parity only, no new features)*

---

## Change-request log (mid-build scope changes start here)

Append first, implement only after re-gating:

| Date | Task/Spec affected | Change requested | Reason | Re-gated through | Status |
|---|---|---|---|---|---|
| 2026-10-22 | (initial scaffold) | Created this tasks.md as part of the spec scaffold approved by user | user-approved overhaul plan | Phase 1+2 done before any checkbox | applied |
| 2026-10-22 | (scaffold — design) | **Stack pivot:** rebuild the desktop app on Spring Boot 4.1.x + OpenJFX 25 + SQLite/Flyway + Maven, Java stays the language | user decision: framework-based overhaul | requirements.md constraints + new R8.11 updated; design.md rewritten around the stack; swing tree frozen to bug-fixes | applied |
| 2026-10-22 | T4 | Twin-core retirement shape settled as **delete**; harness tally 24→12 shipped checks | user delegated the decision; twin never packaged + curve contradiction | requirements.md harness constraint wording updated; spec-workflow checklist de-numbered; design.md had no twin references | applied |
