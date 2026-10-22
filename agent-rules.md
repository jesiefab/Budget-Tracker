# agent-rules.md — The Phase-Gate Contract (Agents Must Follow)

This is the working contract for any agent (or human) implementing from the spec. It is deliberately short and separate from the sheets so the sheets get edited freely and the contract does not get edited to allow scope creep.

## The gate

```
Phase 1  requirements.md   → WHAT & WHY          · NO source edits
Phase 2  design.md         → HOW (trace to R#)   · NO source edits
Phase 3  tasks.md          → checklist           · checked box = authorization to code
```

1. **Never edit source outside Phase 3** — and even in Phase 3, only for a task whose checkbox is currently checked or being checked this turn.
2. **Checkbox = authorization.** No checkbox → no change, not even "small fixes along the way."
3. **Mid-build discovery protocol.** Something new? Before touching code:
   a. Append a row to the [tasks.md](tasks.md) change-request log (date, what, why).
   b. If it changes WHAT the app does → update `requirements.md` first, then `design.md`, then re-gate.
   c. If it changes HOW → update `design.md` first, then implement.
   d. `scripts/check-spec.sh` must still pass after the spec edit.
4. **No invisible side-quests.** "While I'm in here I should also…" is a change request, not a change.
5. **No weakening checks to pass.** Tests, assertions, and `done when` criteria are load-bearing. If a check is actually wrong, fix it via the change-request protocol with a stated reason — not silently.
6. **Per-feature small specs.** Anything too big for `tasks.md` (a new feature) gets its own `specs/<feature>/{requirements,design,tasks}.md` — never a silent expansion of the root spec.

## Session startup

1. Read `requirements.md`, `design.md`, `tasks.md`, this file, `README.md`.
2. Run `./scripts/check-spec.sh` — specs must be internally consistent before you rely on them.
3. **Challenge stale specs.** If a requirement/design no longer matches reality or contradicts another requirement, say so explicitly in your reply and propose the spec fix — do not silently implement stale spec or drift from it.

## Project-specific guardrails (violations are regressions, not style choices)

- **Your user's real save file is off-limits to writes** except through the app itself. Tests run against OS temp dirs with an injected save directory.
- **XP curve is factory-balance** — quadratic, 4500 XP to rank 10 (R4.4). Never linearize to make a test or a design simpler.
- **`Level=` in a save is ignored on load** by design; never reintroduce trusting it (R4.6/R8.9).
- **Escaping order is `\` then `|`**, scan-based split on load (R8.6). Don't "clean this up."
- **27 requirement IDs (R1.1–R8.10) are referenced all over the codebase via comments/test names.** Renumbering breaks traceability and `check-spec.sh`.
- **Compile health = run the harness at least at turn end** (`tests/run-tests.sh` / `run-tests.bat`) — it compiles the full shipped `Desktop/*.java` set; if you changed anything in that tree, exit 0 is part of your diff's correctness.

## Commits

- One task → one commit, referencing the task ID, **only when the user asks for a commit** (project convention: never push or commit proactively).
- Never discard/unstage user-owned edits; leave uncommitted work intact.

## Language & style

- Match existing code style: section-divided comment blocks, defensive `fromString` parsing, listener fan-out from `DataStore`.
- Prose with the user: language of their latest message.
