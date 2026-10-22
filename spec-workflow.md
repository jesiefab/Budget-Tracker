# spec-workflow.md — How We Improve Spec-Driven Coding Here

Why this file exists: the 3-phase scaffold (`requirements.md` → `design.md` → `tasks.md` → phase gate) is the baseline. These 8 practices make the workflow actually hold up over time, especially with AI agents implementing. Each links to where it's baked into this repo.

---

### 1. Small specs beat big specs
A root `requirements.md` is the **baseline contract** for the whole app; but per-feature work is a **separate, small spec**: `specs/<feature>/{requirements,design,tasks}.md`. Same 3-phase gate, narrower scope.
- Big monolithic specs drift out of date and produce uncheckable reviews; small ones stay reviewable.
- Rule: the root spec never changes mid-feature except to reference the new `specs/<feature>/` folder.
- **Baked in:** [agent-rules.md](agent-rules.md) § "Per-feature small specs"; tasks T6–T8 as first rules.

### 2. Decision log
Every non-obvious choice (e.g. "keep the quadratic XP curve, reject the linear one", "write-then-swap instead of in-place write") gets a dated entry stating decision, rationale, impact. Without it, a future agent re-litigates the decision or overrules it silently.
- Per-feature specs keep their own `decision_log.md`. Cross-cutting choices go into [design.md](design.md) § 7 "Known Risks / Do-Not-Touch List" — that section **is** this project's standing decision log.

### 3. Ask questions in Phase 1, not mid-build
`requirements.md` heading must contain an explicit **Open Questions** block. The agent asks there before coding; ambiguity resolved early is cheap, detected mid-task it costs a rollback.

### 4. Automate traceability, don't eyeball it
A tiny script cross-checks R# IDs: every ID used in `design.md` / `tasks.md` exists in `requirements.md`, and every requirement number is used at least once downstream. `scripts/check-spec.sh` exits non-zero on any gap and must be run at session start ([agent-rules.md](agent-rules.md)) and after every spec edit.

### 5. Test-first task ordering
In `tasks.md`, each implementation task's "done when" is expressed as a **pre-existing check in the harness** where possible — the check exists (failing) *before* the code does. This is what already made the persistence fixes verifiable rather than hopeful.

### 6. Re-read the spec before starting a session, and let the agent challenge it
Specs get stale and can even be wrong. The agent's job is to flag that loudly and propose a spec patch through the change request — never to silently implement the stale version or quietly drift from it. An agent is allowed to push back on the spec; it is not allowed to ignore it.
- **Baked in:** [agent-rules.md](agent-rules.md) § "Challenge stale specs".

### 7. Atomic commits per task
One `tasks.md` task = one commit that references the task ID (`T2: rebuild v3.0.1 release assets`). Generates the "why" trail the decision log doesn't (code-history granularity). Partially-done tasks stay uncommitted rather than bundled in with an unrelated one — this is what kept the last port reviewable.

### 8. Update the spec, not just the code
When reality changes mid-build (a formula was wrong, a response shape differs, a requirement turned out unbuildable as written), the change is unapplied spec-first: edit `requirements.md`/`design.md`/`tasks.md` to match reality, then make code match everything again. Any commit whose spec no longer describes what it ships is a defect.

---

## Checklist before a session ends

- [ ] `scripts/check-spec.sh` passes
- [ ] harness (`tests/run-tests.sh`) all checks passing / exit 0 after any code change
- [ ] any discovery change-request-logged
- [ ] any spec edit traceable (`R#` added upstream before appearing downstream)
- [ ] spec patches pushed as their own commit when a task was committed
