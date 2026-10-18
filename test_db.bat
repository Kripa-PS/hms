@echo off
setlocal

echo ============================================================
echo   Testing Hotel Management System Database Connection
echo ============================================================

set "JAVA_CMD=c:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot\bin\java.exe"
if not exist "%JAVA_CMD%" (
    set "JAVA_CMD=java"
)

"%JAVA_CMD%" -cp "bin;lib/mysql-connector-j-9.1.0.jar;lib/flatlaf-3.5.4.jar" com.hms.util.DatabaseConnection
echo.
pause
