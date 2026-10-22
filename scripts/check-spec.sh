#!/usr/bin/env bash
# check-spec.sh — spec-scaffold traceability check for Student Budget Tracker.
# Verifies the 3-phase scaffold is internally consistent:
#   1. all five scaffold files exist
#   2. every design/tasks-cited requirement ID exists in requirements.md
#   3. every requirements group (R1..R8) is used downstream at least once
# Exit 0 = consistent, 1 = gap found. Run from repo root (or anywhere; paths self-locate).

set -u
root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$root"

fail=0
err() { printf 'FAIL: %s\n' "$1" >&2; fail=1; }
ok()  { printf '  ok: %s\n' "$1"; }

# 1. files exist
for f in requirements.md design.md tasks.md agent-rules.md spec-workflow.md scripts/check-spec.sh; do
  if [[ -f $f ]]; then ok "exists: $f"; else err "missing file: $f"; fi
done
[[ $fail -eq 1 ]] && exit 1

# 2. collect IDs declared in requirements.md (e.g. R1.1 .. R8.10)
#    headings/defs look like | R2.3 | ... or ## R2 —
req_ids=$(grep -oE '\bR[0-9]+(\.[0-9]+)?\b' requirements.md | sort -u)
leaf_ids=$(grep -oE '\bR[0-9]+\.[0-9]+\b' requirements.md | sort -u)
[[ -n "$leaf_ids" ]] || err "requirements.md declares no leaf IDs (R1.1 style)"

# 3. every leaf ID cited in design.md / tasks.md exists upstream
for f in design.md tasks.md spec-workflow.md agent-rules.md; do
  cited=$(grep -oE '\bR[0-9]+\.[0-9]+\b' "$f")
  bad=""
  for id in $cited; do
    grep -qE "(^|[^0-9.])$id([^0-9.]|\$)" requirements.md || bad="$bad $id"
  done
  if [[ -z "$bad" ]]; then ok "all cited IDs in $f trace to requirements.md";
  else err "IDs in $f missing from requirements.md:$bad"; fi
done

# 4. every R1..R8 group is used downstream
for g in R1 R2 R3 R4 R5 R6 R7 R8; do
  for f in design.md tasks.md; do
    if grep -qE "\b$g(\.[0-9]+)?\b" "$f"; then grep -qE "\b$g\b" "${f%.md}.md" && break; fi
  done
  grep -qE "\b$g(\.[0-9]+)?\b" design.md || err "group $g untraceable in design.md"
  grep -qE "\b$g(\.[0-9]+)?\b" tasks.md   || err "group $g untraceable in tasks.md"
done

# 5. phase markers present in each sheet
grep -q "Phase 1" requirements.md || err "requirements.md missing Phase-1 marker"
grep -q "Phase 2" design.md      || err "design.md missing Phase-2 marker"
grep -q "Phase 3" tasks.md       || err "tasks.md missing Phase-3 marker"

# optional 6. change-request log exists in tasks.md
grep -qi "change-request log" tasks.md || err "tasks.md missing change-request log section"

if [[ $fail -eq 1 ]]; then
  printf '\ncheck-spec: FAILED — fix the FAIL lines above before relying on the spec.\n'
  exit 1
fi
printf '\ncheck-spec: PASS — scaffold traceability is consistent.\n'
exit 0
