@echo off
rem Compiles the shipped desktop code (Desktop), the refactored core (src\main\java)
rem and the verification harness (tests\Tests.java + tests\DesktopTests.java), then
rem runs it. DesktopTests exercises the shipped code.
setlocal
cd /d "%~dp0.."

dir /s /b src\*.java > "%TEMP%\bt-sources.txt" || exit /b 1
javac -encoding UTF-8 -d tests @"%TEMP%\bt-sources.txt" || exit /b 1
javac -encoding UTF-8 -d tests Desktop\*.java || exit /b 1
javac -encoding UTF-8 -cp tests -d tests tests\*.java || exit /b 1
java -cp tests Tests
exit /b %errorlevel%
