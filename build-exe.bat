@echo off
rem Builds the Windows distribution for Budget Tracker (Desktop app).
rem Run from the repo root:   build-exe.bat
rem
rem Produces:
rem   dist\BudgetTracker\BudgetTracker.exe       self-contained app (bundled Java runtime)
rem   dist\BudgetTracker-3.0.0-Setup.exe         single-file installer (needs WiX Toolset 3.x once)
rem   dist\BudgetTracker-3.0.0-windows-x64.zip   zipped app folder for sharing
rem
rem Requirements: JDK 17+ (javac/jar/jdeps/jpackage on PATH).
setlocal
cd /d "%~dp0"

set APP_NAME=BudgetTracker
set APP_VERSION=3.0.0
set VENDOR=MysteGUI
set DESCRIPTION=Student Budget Tracker
set ICON=Desktop\Savings Tracker Icon.ico
set MODULES=java.base,java.desktop
set WIXBIN=C:\Program Files (x86)\WiX Toolset v3.14\bin

echo [1/6] clean
if exist dist rmdir /s /q dist
mkdir dist\classes
mkdir dist\jarinput

echo [2/6] compile Desktop sources
javac -encoding UTF-8 -d dist\classes Desktop\*.java || goto :fail

echo [3/6] package jar
jar --create --file "dist\jarinput\%APP_NAME%.jar" --main-class Main -C dist\classes . || goto :fail

echo [4/6] app-image (self-contained, no Java needed on the target PC)
jpackage --type app-image --name %APP_NAME% --app-version %APP_VERSION% ^
  --vendor %VENDOR% --description "%DESCRIPTION%" --copyright "Copyright 2025 %VENDOR%" ^
  --input dist\jarinput --main-jar %APP_NAME%.jar --main-class Main ^
  --icon "%ICON%" --add-modules %MODULES% --dest dist || goto :fail

echo [5/6] zip the app folder
powershell -NoProfile -Command "Compress-Archive -Path 'dist\%APP_NAME%' -DestinationPath 'dist\%APP_NAME%-%APP_VERSION%-windows-x64.zip' -Force" || goto :fail

echo [6/6] single-file installer (requires WiX Toolset 3.x)
if not exist "%WIXBIN%\candle.exe" (
  echo       SKIPPED: WiX Toolset 3.x not found.
  echo       Install it once ^(admin^):  winget install --id WiXToolset.WiXToolset -e
  echo       Then re-run this script. The app-image and zip above are already built.
  goto :done
)
set PATH=%WIXBIN%;%PATH%
jpackage --type exe --name %APP_NAME% --app-version %APP_VERSION% ^
  --vendor %VENDOR% --description "%DESCRIPTION%" --copyright "Copyright 2025 %VENDOR%" ^
  --input dist\jarinput --main-jar %APP_NAME%.jar --main-class Main ^
  --icon "%ICON%" --add-modules %MODULES% ^
  --win-menu --win-shortcut --win-per-user-install --dest dist || goto :fail
move /y "dist\%APP_NAME%-%APP_VERSION%.exe" "dist\%APP_NAME%-%APP_VERSION%-Setup.exe" >nul

:done
echo.
echo Artifacts:
dir /b dist\*.zip dist\*-Setup.exe 2>nul
echo App folder: dist\%APP_NAME%
exit /b 0

:fail
echo BUILD FAILED (exit code %errorlevel%)
exit /b %errorlevel%
