#!/usr/bin/env bash
# Compiles the refactored core (src/main/java) plus the audit-fix verification
# harness (tests/Tests.java) and runs it from the repo root.
#
# Exit code 0 = every check passed, 1 = at least one failure.
set -euo pipefail
cd "$(dirname "$0")/.."

javac -encoding UTF-8 -d tests $(find src -name '*.java')
javac -encoding UTF-8 -cp tests -d tests tests/Tests.java
java -cp tests Tests
