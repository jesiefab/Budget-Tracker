@echo off
rem Compiles the shipped desktop code (Desktop) and the verification harness
rem (tests\Tests.java + tests\DesktopTests.java), then runs it. DesktopTests
rem exercises the shipped code; the retired core tree was removed in T4.
setlocal
cd /d "%~dp0.."

javac -encoding UTF-8 -d tests Desktop\*.java || exit /b 1
javac -encoding UTF-8 -cp tests -d tests tests\*.java || exit /b 1
java -cp tests Tests
exit /b %errorlevel%
