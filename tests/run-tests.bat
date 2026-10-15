@echo off
rem Compiles the refactored core (src\main\java) plus the audit-fix verification
rem harness (tests\Tests.java) and runs it.
rem Exit code 0 = every check passed, 1 = at least one failure.
setlocal
cd /d "%~dp0.."

dir /s /b src\*.java > "%TEMP%\bt-sources.txt" || exit /b 1
javac -encoding UTF-8 -d tests @"%TEMP%\bt-sources.txt" || exit /b 1
javac -encoding UTF-8 -cp tests -d tests tests\Tests.java || exit /b 1
java -cp tests Tests
exit /b %errorlevel%
