@echo off
echo ============================================================
echo   Compiling Hotel Management System with JDK 25
echo ============================================================

set JAVAC_CMD="c:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot\bin\javac.exe"
if not exist %JAVAC_CMD% (
    set JAVAC_CMD=javac
)

if not exist "bin" mkdir bin

%JAVAC_CMD% -d bin -cp "lib/mysql-connector-j-9.1.0.jar;lib/flatlaf-3.5.4.jar" src/main/java/com/hms/model/*.java src/main/java/com/hms/dao/*.java src/main/java/com/hms/util/*.java src/main/java/com/hms/ui/*.java src/main/java/com/hms/*.java src/test/java/com/hms/model/*.java src/test/java/com/hms/dao/*.java src/test/java/com/hms/ui/*.java src/test/java/com/hms/util/*.java

if %ERRORLEVEL% EQU 0 (
    echo [SUCCESS] Compilation finished with 0 errors!
) else (
    echo [ERROR] Compilation failed.
)
pause
