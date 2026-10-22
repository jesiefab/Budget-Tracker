# requirements.md — Student Budget Tracker (Full-App Scope)

> **Phase 1 of 3.** This file defines **WHAT** we build and why. It contains no design decisions.
> Agents: you may NOT edit source code while working in this phase. See [agent-rules.md](agent-rules.md).
>
> - `R1.x` Expense Tracking · `R2.x` Budget · `R3.x` Savings · `R4.x` XP & Ranking
> - `R5.x` History · `R6.x` Themes · `R7.x` Settings · `R8.x` Persistence & Data Integrity
>
> Every ID here must be traceable in [design.md](design.md) and [tasks.md](tasks.md).
> Run `./scripts/check-spec.sh` to verify traceability after any edit.

---

## Project Goal

A budgeting app built for **Filipino students** to track expenses (₱), manage budgets, and build saving habits — gamified with 10 ranking titles to stay motivated.

**Target user:** a student with no accounting background, a phone or Windows laptop, and no reliable internet.

**Product principles**

1. **Offline-first** — no account, no network calls, no telemetry. All data lives on the user's device.
2. **Datas are sacred** — a save file represents weeks of the user's diligence. Data loss is the single worst defect this app can ship; persistence requirements (R8) always outrank features.
3. **Faithful balance** — the shipped XP curve is deliberate game balance. Requirements do not change shipped behavior; they document and protect it.
4. **Plain text over magic** — saves are human-readable files a student can open in Notepad.

---

## R1 — Expense Tracking

| ID | Requirement |
|---|---|
| R1.1 | The user logs an expense with a description, an amount in ₱, a category, and a **Need / Want** tag, through a modal overlay dialog. Date defaults to the entry date. |
| R1.2 | The Need/Want tag chosen at entry time is authoritative and feeds Advanced budget calculations (R2.5) — no category-to-type guessing table may exist anywhere. |
| R1.3 | Each list entry shows its `[Need]` / `[Want]` tag inline. |
| R1.4 | The expense list supports live text search on description and a category filter; the running total and count reflect **only the filtered results**. |
| R1.5 | The user can delete individual expenses, with a confirmation prompt before deletion. |
| R1.6 | The user can edit an expense in place; the edited entry keeps its position in the list. |

## R2 — Budget Management

| ID | Requirement |
|---|---|
| R2.1 | The user sets a **Weekly**, **Monthly**, or **Yearly** budget amount. |
| R2.2 | Only expenses logged within the **current budget period** count toward budget numbers; prior-period expenses never affect them. |
| R2.3 | Proration uses **real calendar lengths** — month lengths of 28–31 days (leap years included) and 365/366-day years — never hardcoded 30/365. All six derived formulas and the `Day X of N` display must agree with the same source of truth. |
| R2.4 | **Basic mode**: one progress bar with color-coded warnings (green → yellow → red) plus four stat cards: This Period Spent, Prorated Budget, Today's Buffer, Safe to Spend. |
| R2.5 | **Advanced mode** splits the budget into **Needs** and **Wants** pools based on each expense's actual Need/Want tag, each pool with its own progress bar and stat cards. A Basic/Advanced toggle sits below the donut chart. |
| R2.6 | The Needs/Wants split is a configurable percentage; each pool's budget equals the period budget × that percentage. |
| R2.7 | A **spending breakdown donut chart** visualizes all-time spending by category with a color-coded legend, updating live as expenses change. |

## R3 — Savings Tracker

| ID | Requirement |
|---|---|
| R3.1 | The user logs deposits and withdrawals to a savings pool. |
| R3.2 | The user manages **multiple named savings goals**, each with its own target amount and progress bar. |
| R3.3 | All goals share one `currentSavings` pool; a goal's progress is `pool / target`, displayed capped at 100%. |
| R3.4 | Claiming a goal subtracts the goal's target from the pool and marks it achieved — only when not yet achieved and only when the pool covers the target. |
| R3.5 | Goals can be removed at any time. |
| R3.6 | A legacy single savings goal + pool (pre-multi-goal saves) keeps loading and stays editable. |

## R4 — XP & Ranking System

| ID | Requirement |
|---|---|
| R4.1 | Deposits award XP: +1 XP per ₱10 saved, minimum +5 XP per deposit. |
| R4.2 | Withdrawals deduct 1 XP per ₱1 withdrawn (minimum 1 XP), can trigger a **level-down**, and never push total XP or level 1 below zero/floor. Savings cannot go negative. |
| R4.3 | Ten rank titles, in order: Broke Boy, Budget Apprentice, Penny Pincher, Frugal Warrior, Savings Knight, Money Mage, Budget Royalty, Wealth Sage, Diamond Saver, Financial Legend. |
| R4.4 | The curve **scales harder each level**: the threshold to complete level *L* is `100 × L` XP, thresholds cumulative from level 1. Reaching rank 10 costs 4500 total XP. This is shipped game balance — a "simpler" linear curve is a defect, not a cleanup. |
| R4.5 | Level-up plays an animation and toast notification; the XP bar shows `current / needed` XP and resets per level. |
| R4.6 | On load, level is **always derived from total XP** (`levelFromXP`). A stale, hand-edited, or missing `LEVEL=` save line must never desync the level bar or title. |

## R5 — Expense History

| ID | Requirement |
|---|---|
| R5.1 | A full table of all logged expenses: date, Need/Want type (color-coded), category, description, amount. |
| R5.2 | A category breakdown panel shows per-category totals with progress bars and percentages. |
| R5.3 | The user can clear all history, with confirmation. |

## R6 — Themes

| ID | Requirement |
|---|---|
| R6.1 | Five built-in themes: **Dark** (default), Light, Midnight Blue, Forest Green, Warm Sunset. |
| R6.2 | Switching a theme restyles the entire running UI **instantly**, without a restart or re-launch. |
| R6.3 | The chosen theme is persisted and restored on the next launch. |

## R7 — Settings

| ID | Requirement |
|---|---|
| R7.1 | Settings open via the **gear icon** on the far right of the top navigation bar; Settings is not a nav tab. |
| R7.2 | Settings hosts the theme switcher (R6). |
| R7.3 | An **Admin Tools** area — password-protected — can independently reset: savings, XP, expenses, and budget (for testing). No accidental reset possible. |

## R8 — Persistence & Data Integrity

| ID | Requirement |
|---|---|
| R8.1 | All data auto-saves to a plain-text file in the user's home directory (desktop default: `budget_tracker_save.dat`), every time the app closes. No manual save button. |
| R8.2 | Saving must be **atomic**: write the complete new state to a temp file, then atomically swap it over the live file. A crash mid-save can never tear the live save. |
| R8.3 | The previous save becomes `.bak` only **after** the new write succeeds. The on-disk pair must always hold at least one fully-complete state. |
| R8.4 | If the live `.dat` is missing, the app **falls back to `.bak`** on startup instead of silently starting fresh. |
| R8.5 | A file with no recognizable save keys, or unparseable numeric fields, is treated as **corrupt** (never silently half-loaded); the recovery/backup path engages. |
| R8.6 | Values may contain `|` and `\`. Encoding escapes `\` first, then `|`; decoding is scan-based and lossless. Legacy (v2-era) saves decode byte-identically to the old behavior. |
| R8.7 | Save files from v2.0 load without issue; expenses missing a Need/Want tag default to **Need**. |
| R8.8 | Restore is **buffered**: nothing touches the live store until the file fully parses. |
| R8.9 | A save-file `LEVEL=` line is advisory at best — load must derive level from XP (see R4.6). |
| R8.10 | The persistence layer must support an injected save directory / connection so the automated harness can exercise real save-load paths without touching the user's actual data. |
| R8.11 | The first run of the rebuilt (framework-stack) app must **import a legacy v3 plaintext save losslessly** into its database — including escaped values and v2-era saves. The legacy file is preserved (renamed, never deleted), and re-running the import must not duplicate data. |

---

## Constraints

- **Platform:** the rebuilt app targets **Windows desktop** with a fully bundled runtime (no Java installed on the user's machine); Android is a later milestone and requirements must stay expressible there. The legacy Swing version in `Desktop/` is source-frozen (bug fixes only) once the rebuild starts.
- **Money is ₱ (PHP)** throughout; no multi-currency.
- **No network code anywhere.** No cloud sync, no accounts, no crash reporters.
- **Stack (approved pivot, 2026-10-22):** Java stays the language. The rebuilt desktop app runs on **Spring Boot 4.1.x** (application core) + **OpenJFX 25 / JavaFX** (presentation layer) + **SQLite via sqlite-jdbc with Flyway migrations** (persistence) + **Maven** (build). The legacy Swing app stays frozen in `Desktop/` until the rebuild reaches feature parity — see [design.md](design.md) § Stack and § Migration.
- The automated harness ([tests/Tests.java](tests/Tests.java)) must stay green (24/24) after every change; it compiles the real shipped sources.

## In Scope / Out of Scope

| In scope | Out of scope |
|---|---|
| All feature areas R1–R8 on desktop | Cloud sync, online accounts, telemetry |
| Android parity work that mirrors existing desktop behavior | New mobile-only feature design |
| Save-format hardening and forward/backward compatibility | Multi-user or networked operation of any kind |
| Re-platforming onto the approved stack (Spring Boot + JavaFX + SQLite) incl. legacy-save import (R8.11) | Changing the shipped XP balance (R4.4) as part of the pivot |
| Packaging (installer / portable app image) via `jpackage` + WiX | Play Store / Microsoft Store publishing |
| Localization of the information architecture (not yet translation) | Translation to other languages |

## Acceptance Criteria (checkable statements)

- Add expense → it appears with the correct tag, and Basic/Advanced budget numbers move within the period. (R1.1–R1.3, R2.2, R2.5)
- Filter the list → total and count shrink to the filtered set. (R1.4)
- February 15th proration ≈ half of a 28-day month, and leap-Feb differs from 365-day years. (R2.3)
- Deposit ₱500 → ≥ +5 XP; withdraw the same ₱500 → exactly −500 XP, possibly a level-down. (R4.1–R4.2)
- Hand-edit `LEVEL=99` into a save → app still shows the correct derived level. (R4.6, R8.9)
- Kill the app mid-save → the previous `.dat`/`.bak` pair still loads complete data. (R8.2–R8.3)
- Rename `.dat` away, keep `.bak` → the app restores from `.bak`, not from blank. (R8.4)
- Fill a save with garbage → the app treats it as corrupt and follows the recovery path. (R8.5)
- Save an expense description with pipes/backslashes → reload reproduces it exactly. (R8.6, R1.1)
- Change theme → every visible surface restyles immediately, and it survives a restart. (R6.1–R6.3)
- `tests/run-tests.sh` and `run-tests.bat` exit 0 with all checks passing. (all)
- Point the rebuilt app at an existing `budget_tracker_save.dat` → every expense, goal, deposit and setting appears intact; the legacy file is preserved and a second import doesn't duplicate anything. (R8.11)
