@echo off
setlocal

echo ============================================================
echo   Hotel Management System - Database Initializer
echo ============================================================
echo.

rem 1. Locate mysql.exe safely handling spaces in path
set "MYSQL_EXE="
if exist "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe" (
    set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe"
) else if exist "C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe" (
    set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 9.0\bin\mysql.exe"
) else if exist "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe" (
    set "MYSQL_EXE=C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"
) else (
    for /f "delims=" %%I in ('where mysql.exe 2^>nul') do set "MYSQL_EXE=%%I"
)

if not defined MYSQL_EXE (
    echo [ERROR] MySQL Server was not found in standard installation paths.
    echo Please make sure MySQL Server is installed and running.
    echo.
    pause
    exit /b 1
)

echo Found MySQL at: "%MYSQL_EXE%"
echo.
echo Please enter your MySQL 'root' password when prompted.
echo (The password will not be displayed on the screen as you type)
echo.

rem 2. Run schema.sql
echo [1/2] Executing sql\schema.sql to create database and tables...
"%MYSQL_EXE%" -u root -p < "sql\schema.sql"
if errorlevel 1 (
    echo.
    echo [ERROR] Failed to execute schema.sql. Please check your password.
    pause
    exit /b 1
)
echo [SUCCESS] Database 'hotel_management' and tables created!
echo.

rem 3. Run seed_data.sql
echo [2/2] Executing sql\seed_data.sql to insert sample records...
"%MYSQL_EXE%" -u root -p < "sql\seed_data.sql"
if errorlevel 1 (
    echo.
    echo [ERROR] Failed to execute seed_data.sql.
    pause
    exit /b 1
)
echo [SUCCESS] Sample rooms, staff accounts, and test data seeded!
echo.

echo ============================================================
echo   Database Setup Completed Successfully!
echo ============================================================
echo.
echo IMPORTANT NEXT STEP:
echo Make sure your local 'db.properties' file contains your password:
echo   db.url=jdbc:mysql://localhost:3306/hotel_management?useSSL=false^&serverTimezone=UTC^&allowPublicKeyRetrieval=true
echo   db.user=root
echo   db.password=YOUR_PASSWORD
echo.
pause
