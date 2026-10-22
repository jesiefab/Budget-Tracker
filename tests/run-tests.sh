#!/usr/bin/env bash
# Compiles the shipped desktop code (Desktop), the refactored core (src/main/java)
# and the verification harness (tests/Tests.java + tests/DesktopTests.java), then
# runs it from the repo root. DesktopTests exercises the shipped code.
#
# Exit code 0 = every check passed, 1 = at least one failure.
set -euo pipefail
cd "$(dirname "$0")/.."

javac -encoding UTF-8 -d tests $(find src -name '*.java')
javac -encoding UTF-8 -d tests Desktop/*.java
javac -encoding UTF-8 -cp tests -d tests tests/*.java
java -cp tests Tests
