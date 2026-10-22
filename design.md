# design.md — Student Budget Tracker v4 (How We Build It)

> **Phase 2 of 3.** Maps every requirement in [requirements.md](requirements.md) to the approved framework stack. Every design element cites `R#`; nothing untraceable belongs here, and nothing in requirements may lack a home here.
>
> **No source code may be edited in this phase.** Implementation authorization lives in checked tasks in [tasks.md](tasks.md) — see [agent-rules.md](agent-rules.md).

---

## 1. Approved Stack (framework build)

Approved by user pivot, 2026-10-22. Java remains the language; the Swing app in `Desktop/` is frozen to bug-fixes only once T9 begins.

| Layer | Choice | Version (current, late 2026) | Why / notes |
|---|---|---|---|
| Language / runtime | **JDK 25 (LTS)** | 25 | JavaFX 25 line requires JDK 23+; coherent LTS pairing |
| Application core | **Spring Boot** | 4.1.1 | DI container, events, config, test tooling; runs `WebApplicationType.NONE` |
| Presentation | **OpenJFX / JavaFX** + FXML | 25.0.5 (LTS line) | Declarative views, CSS theming, no Swing |
| Persistence | **SQLite** via `sqlite-jdbc` + **Spring JDBC** (`JdbcTemplate`) + **Flyway** | sqlite-jdbc 3.53.4.0 | Embedded, zero-admin, single-file DB |
| Build | **Maven** + `javafx-maven-plugin` | plugin 0.0.8 | Widely documented; no Gradle migration complexity |
| Tests | `spring-boot-starter-test` (JUnit 5, AssertJ) | Boot-managed | Service + persistence tests, no JavaFX needed |
| Packaging | `jpackage` app image + WiX installer | JDK-bundled | Replaces current jpackage flow |

**Deliberately NOT chosen** (researched, sources below):

- **Spring Data JPA on SQLite** — Hibernate's SQLite dialect lives in `hibernate-community-dialects` (best-effort, community-maintained; Hibernate 7.4.x). Workable but weak support.
- **Spring Data JDBC** — maintainers declined a SQLite dialect (spring-data-relational#1769); no built-in dialect.
- **jOOQ** — good typed SQL, but code-gen ceremony not worth it for a ~10-table single-user app.
- **H2 as the production DB** — SQLite-specific SQL and behavior differ; we develop and test against SQLite itself.
- **Bridge starters** (`yoep/spring-boot-starter-javafx` 2.0.0, stale Dec-2023; FxWeaver 2.0.1 exists but no Boot-4 guarantee) — a ~20-line lifecycle adapter (§2) is the whole job; we own it instead.
- **TestFX** — usable (4.0.18, "legacy support" per its README); allowed for a *small* number of UI smoke tests only, never as the core test strategy.

Sources: BellSoft Spring Boot+JavaFX tutorial (bell-sw.com/blog/creating-modern-desktop-apps-with-javafx-and-spring-boot/), OpenJFX Application javadoc / openjfx.io, Spring Boot 4.1.1 release-announcement + system-requirements (spring.io / docs.spring.io), Gluon OpenJFX product table (gluonhq.com/products/javafx), Flyway SQLite reference (documentation.red-gate.com/fd/sqlite-277579331.html), spring-data-relational#1769, TestFX README, Oracle jpackage overview.

## 2. Bootstrap & Lifecycle (Spring ↔ JavaFX)

The one piece of glue we own — JavaFX owns the desktop lifecycle, Spring owns the context:

```java
@SpringBootApplication
public class BudgetTrackerApp extends Application {
    private ConfigurableApplicationContext context;

    public static void main(String[] args) {
        Application.launch(BudgetTrackerApp.class, args);   // JavaFX launcher thread
    }

    @Override public void init() {                            // launcher thread
        context = new SpringApplicationBuilder(BudgetTrackerApp.class)
                .web(WebApplicationType.NONE)
                .run(getParameters().getRaw().toArray(String[]::new));
    }

    @Override public void start(Stage stage) {                // FX Application Thread
        context.getBean(UiLauncher.class).show(stage);        // splash → main window
    }

    @Override public void stop() {                            // launcher thread, on exit
        if (context != null) context.close();                 // flush + close DB here (R8.1)
    }
}
```

Rules:

- `init()` is Spring's only job; **no Stage/Scene construction there**. All scene-graph work happens on the FX thread in `start()` or later (`Platform.runLater` from services).
- The JavaFX launcher constructs this class itself — Spring does not create it. DI into it is done by hand (two fields), everything else comes from `context.getBean(...)`.

## 3. Architecture Slices

```
app/
├── pom.xml
└── src/main/java/com/mystegui/budgettracker/
    ├── BudgetTrackerApp.java        → §2 bootstrap
    ├── UiLauncher.java              → splash + FXML stage assembly
    ├── domain/                      → pure services, no UI/DB types in signatures
    │   ├── ExpenseService.java      → R1.1, R1.2, R1.5, R1.6
    │   ├── BudgetService.java       → R2.1–R2.6 (math unchanged — see §6)
    │   ├── SavingsService.java      → R3.1–R3.6
    │   ├── XpService.java           → R4.1–R4.6 (curve is frozen balance — see §7)
    │   └── ImportService.java       → R8.11 legacy .dat import
    ├── persistence/                 → Spring JDBC repositories + DTOs
    │   ├── ExpenseRepository.java / SavingsGoalRepository.java / SettingsRepository.java
    │   └── db/migration/V1__schema.sql  → Flyway baseline
    ├── ui/                          → FXML controllers (prototype-scoped, see §5)
    └── config/                      → AppProps (data dir), fx thread config
└── src/main/resources/
    ├── fxml/*.fxml                  → expenses, budget, savings, history, settings views
    ├── css/{dark,light,midnight-blue,forest-green,warm-sunset}.css
    └── db/migration/
```

**Spring `ApplicationEvent` replaces `DataStore` listener fan-out.** Panel refresh is via `@EventListener` on domain events (`ExpenseAdded`, `GoalClaimed`, `XpAwarded`…) so a UI change never reaches into another slice. `forceRefresh()` becomes a single `ThemeChanged` event (R6.2).

## 4. Persistence (R8.x)

**Database file:** `%APPDATA%/BudgetTracker/budget_tracker.db` (OS app-data dir — never inside `Program Files`; `AppProps` under §3 owns the path, injected in every test, satisfying R8.10).

Schema baseline — Flyway `V1__schema.sql` (migration replaces SaveManager; broken/interrupted schema updates are what `flyway history` exists for):

```sql
CREATE TABLE settings    (key TEXT PRIMARY KEY, value TEXT NOT NULL);          -- R6.3, R7
CREATE TABLE expenses    (id INTEGER PRIMARY KEY AUTOINCREMENT,
                          description TEXT NOT NULL, amount_cents INTEGER NOT NULL,
                          category TEXT NOT NULL, spent_on TEXT NOT NULL,
                          need_want TEXT NOT NULL CHECK (need_want IN ('Need','Want')),
                          created_at INTEGER NOT NULL);
CREATE TABLE savings_pool(id INTEGER PRIMARY KEY CHECK (id = 1),
                          balance_cents INTEGER NOT NULL);                     -- R3.3 shared pool
CREATE TABLE savings_goals(id INTEGER PRIMARY KEY AUTOINCREMENT,
                          name TEXT NOT NULL, target_cents INTEGER NOT NULL,
                          achieved INTEGER NOT NULL DEFAULT 0,                 -- R3.2–R3.4
                          claimed_at INTEGER);                                 -- R3.6 legacy single goal imported here
CREATE TABLE xp_state    (id INTEGER PRIMARY KEY CHECK (id = 1),
                          total_xp INTEGER NOT NULL,
                          imported_level INTEGER);                             -- cur: level is DERIVED (R4.6), column kept only for parity w/ legacy file
```

Details that carry the data-integrity requirements across the pivot:

- **Money as integer cents** everywhere (`amount_cents`), not doubles.
- The SQLite connection lives on the Spring context; `stop()` closes it **after** pending writes commit (R8.1 auto-save-on-close, no manual save).
- SQLite's journal + transaction gives the same end-state guarantees as the old temp-file + `ATOMIC_MOVE` dance (R8.2–R8.4: a killed process leaves the DB at its last committed transaction; there is no torn half-file to recover from). Tests still prove the behavior: kill-after-write can't lose the previous commit.
- **R8.11 legacy import:** `ImportService` reads the v3 plaintext save (keys, pipe-separated expense records, `\`→`\\`, `|`→`\|` escaping order, v2-era 4-field records defaulting `needWant` to 'Need'), inserts rows in one transaction, marks `settings.imported_from = 'dat-v3'`, then **renames** `budget_tracker_save.dat` → `budget_tracker_save.dat.imported` (never deletes). Re-import is a no-op when `imported_from` is set.
- **Levels:** still derived from total XP at startup — the `xp_state.imported_level` column exists only to record what the legacy file claimed, for the import report, and is **never** authoritative (R4.6, R8.9).

## 5. JavaFX Presentation (R1–R7 UI)

- Views are pure **FXML**, one controller per tab, loaded through `FXMLLoader.setControllerFactory(context::getBean)` — controllers are Spring beans and get services via constructor injection.
- **Controller scope:** stateful/tab-scoped controllers are `@Scope("prototype")`, not singleton, so re-loading a view can't leak or reuse stale UI state. Services stay singleton.
- **Theming (R6.1–R6.3):** five theme stylesheet files; switching = swapping the scene's stylesheets + firing `ThemeChanged`. No restart needed.
- Modals (add/edit expense, R1.1/R1.6) are staged overlays; search/filter boxes bind to `ExpenseService` filtered queries (R1.4); BudgetPanel renders all six stat formulas from `BudgetService` (R2.3–R2.6) plus a donut chart backed by `getTotalByCategory`-equivalent queries (R2.7).
- Level-up animation/toast (R4.5) is an FX-timeline overlay fired by an `XpAwarded` event listener.
- Admin tools (R7.3) call the four independent reset service methods, password-gated in the controller layer.

## 6. Domain Math (frozen semantics, new home)

Port verbatim from the existing logic — tests-first (T11) must reproduce current edge cases before UI consumes them:

- **Budget (R2.3–R2.6):** `prorated = (budget / daysInPeriod) × max(1, daysElapsed)`; `dailyAllowance = max((budget − spentThisPeriod) / daysLeft, 0)`; `daysInPeriod` = 7 / **`LocalDate.lengthOfMonth()` (leap-aware)** / `lengthOfYear()`; only expenses with `spent_on ≥ periodStart` count (R2.2); Needs/Wants pools split by configurable percent (R2.6) and filter on the row's own tag (R1.2, R2.5) — no category→type table may exist.
- **XP (R4.1–R4.6):** deposit = `max(5, floor(₱ / 10))`; withdrawal = `max(1, floor(₱ / 1))`, floored at 0; thresholds cumulative `Σ_{i<L} 100·i`, next-level need = `100 × L`; **rank 10 = 4500 XP total — deliberately quadratic, never "simplify" to linear** (R4.4). Level always `levelFromXP(totalXP)`.

## 7. Known Risks / Do-Not-Touch List

| Risk | Guardrail |
|---|---|
| Moving off SQLite guarantees when someone later "optimizes" persistence | R8.2–R8.4 tests keep proving kill-safety against the real DB file |
| Re-trusting stored level data during import | R4.6/R8.9 — `imported_level` is for the report; `levelFromXP` stays authoritative |
| Escapes handling errors in the import (wrong order, greedy split) | R8.6/R8.11 — `\` then `|`, scanner split; round-trip tests on `"a\|b"`, `"a\\b|c"`, trailing `\` |
| Test suite weakly "passes against H2" instead of SQLite | R8.10+T10 — persistence tests use real temp SQLite files + production migrations |
| Bridge/starter libraries silently violating Boot-4/JavaFX-25 | We own the §2 adapter; no third-party FXML-DI starter without re-proof |
| Linearizing XP to make a test simpler | R4.4 — shipped game balance (4500 XP to rank 10) |
| Category→Need/Want mapping creeping back in via "convenience" | R1.2 — the tag entered at entry time is the only source |
| Spring fat-jar breaking JavaFX module loading in the packaged app | §8 packaging shape — verify in the built image, not just from the IDE |

## 8. Windows Packaging (T13)

Recommended (researched) build shape — the middle ground that avoids both Spring-boot-repackaged-fat-jar and full-JPMS risk:

- Spring + app classes on the **ordinary classpath** as plain jars;
- JavaFX modules explicitly on the **module path** and included in the `jlink`/appid runtime via the app-image;
- a small launcher class that does **NOT extend `javafx.application.Application`** (it forwards to `BudgetTrackerApp`), so `jpackage`'s module machinery never has to find JavaFX inside a Boot archive;
- `jpackage` → app-image → WiX installer (per-user, no admin, bundled runtime — same UX as the current Swing releases);
- JDK 25 changed `jpackage` runtime-image service-binding defaults — verify service-loader-dependent deps in the built image (Oracle jpackage overview, add `--jlink-options --bind-services` if a dep needs it);
- DB path (§4) resolves to `%APPDATA%` at runtime; installer writes nothing outside the install dir.

## 9. Traceability Table

| Requirement | New-stack home |
|---|---|
| R1.1–R1.6 expenses | `ExpenseService`, `ExpenseRepository`, expenses FXML view + modal (§5) |
| R2.1–R2.6 budget math | `BudgetService` formulas (§6) + `BudgetService`-fired events → BudgetPanel |
| R2.7 donut chart | BudgetPanel FXML chart fed by category-total query |
| R3.1–R3.6 savings | `SavingsService` + pool row (§4) + goals table + SavingsPanel FXML |
| R4.1–R4.6 XP | `XpService` (§6); derived level (§4); `XpAwarded` event = animation/toast trigger (§5) |
| R5.1–R5.3 history | HistoryPanel FXML table + category breakdown (§5); clear-all asserts via `ExpenseService` |
| R6.1–R6.3 themes | 5 CSS files, live-stylesheet swap, persisted in `settings` (§4, §5) |
| R7.1–R7.3 settings | SettingsPanel FXML + gear-nav; 4 independent admin reset methods |
| R8.1–R8.10 persistence | SQLite + Flyway + Spring JDBC transactional/close semantics (§4) |
| R8.11 legacy import | `ImportService` + settings.imported_from marker + file rename (§4, T13) |

## 10. Migration & Build Order

1. T9 scaffold + bootstrap (context boots, window opens, smoke-test green).
2. T10 persistence slice, tests against real temp SQLite files — **before any UI**, because data-integrity rules are the hard part and test headlessly.
3. T11 domain math port tests-first (XP curve + proration parity before UI consumes anything).
4. T12 JavaFX views over real services; each requirement's acceptance criterion demonstrable.
5. T13 legacy .dat import + jpackage/WiX on a clean Windows machine.
6. Only after parity: legacy Swing tree removed (or archived) — tracked separately in tasks.md T4.
