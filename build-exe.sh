#!/usr/bin/env bash
# Builds the Windows distribution for Budget Tracker (Desktop app).
# Run from the repo root in Git Bash:   ./build-exe.sh
#
# Produces:
#   dist/BudgetTracker/BudgetTracker.exe       self-contained app (bundled Java runtime)
#   dist/BudgetTracker-3.0.0-Setup.exe         single-file installer (needs WiX Toolset 3.x once)
#   dist/BudgetTracker-3.0.0-windows-x64.zip   zipped app folder for sharing
#
# Requirements: JDK 17+ (javac/jar/jdeps/jpackage on PATH). The app itself needs no Java
# on the machine that runs it - the runtime is bundled.
set -euo pipefail
cd "$(dirname "$0")"

APP_NAME="BudgetTracker"
APP_VERSION="3.0.0"
VENDOR="MysteGUI"
DESCRIPTION="Student Budget Tracker"
ICON="Desktop/Savings Tracker Icon.ico"
MODULES="java.base,java.desktop"
WIXBIN="/c/Program Files (x86)/WiX Toolset v3.14/bin"

echo "[1/6] clean"
rm -rf dist
mkdir -p dist/classes dist/jarinput

echo "[2/6] compile Desktop sources"
javac -encoding UTF-8 -d dist/classes Desktop/*.java

echo "[3/6] package jar"
jar --create --file "dist/jarinput/$APP_NAME.jar" --main-class Main -C dist/classes .

echo "[4/6] app-image (self-contained, no Java needed on the target PC)"
jpackage --type app-image --name "$APP_NAME" --app-version "$APP_VERSION" \
  --vendor "$VENDOR" --description "$DESCRIPTION" --copyright "Copyright 2025 $VENDOR" \
  --input dist/jarinput --main-jar "$APP_NAME.jar" --main-class Main \
  --icon "$ICON" --add-modules "$MODULES" --dest dist

echo "[5/6] zip the app folder"
powershell -NoProfile -Command "Compress-Archive -Path 'dist\\$APP_NAME' -DestinationPath 'dist\\$APP_NAME-$APP_VERSION-windows-x64.zip' -Force"

echo "[6/6] single-file installer (requires WiX Toolset 3.x)"
if [ -f "$WIXBIN/candle.exe" ] || command -v candle.exe >/dev/null 2>&1; then
  [ -d "$WIXBIN" ] && PATH="$WIXBIN:$PATH"
  jpackage --type exe --name "$APP_NAME" --app-version "$APP_VERSION" \
    --vendor "$VENDOR" --description "$DESCRIPTION" --copyright "Copyright 2025 $VENDOR" \
    --input dist/jarinput --main-jar "$APP_NAME.jar" --main-class Main \
    --icon "$ICON" --add-modules "$MODULES" \
    --win-menu --win-shortcut --win-per-user-install --dest dist
  mv -f "dist/$APP_NAME-$APP_VERSION.exe" "dist/$APP_NAME-$APP_VERSION-Setup.exe"
  echo "      -> dist/$APP_NAME-$APP_VERSION-Setup.exe"
else
  echo "      SKIPPED: WiX Toolset 3.x not found."
  echo "      Install it once (admin):  winget install --id WiXToolset.WiXToolset -e"
  echo "      Then re-run this script. The app-image and zip above are already built."
fi

echo
echo "Artifacts:"
ls -la dist/*.zip dist/*-Setup.exe 2>/dev/null || true
echo "App folder: dist/$APP_NAME"
