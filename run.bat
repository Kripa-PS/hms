@echo off
echo ============================================================
echo   Starting Grand Horizon Hotel Management System (Java 25)
echo ============================================================

set JAVA_CMD="c:\Program Files\Eclipse Adoptium\jdk-25.0.4.7-hotspot\bin\java.exe"
if not exist %JAVA_CMD% (
    set JAVA_CMD=java
)

%JAVA_CMD% --enable-native-access=ALL-UNNAMED -cp "bin;lib/mysql-connector-j-9.1.0.jar;lib/flatlaf-3.5.4.jar" com.hms.Main
pause
