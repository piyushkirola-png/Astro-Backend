@echo off
echo   Astrology Backend - Startup Script
echo.

echo [1/3] Creating Database and Tables...
mysql -u root -p12345 < src\main\resources\db\schema.sql
if %errorlevel% neq 0 (
    echo ERROR: Failed to create database
    pause
    exit /b 1
)
echo       Database ready.
echo.

echo [2/3] Starting Spring Boot (DataSeeder will hash and insert admin)...
echo.

echo [3/3] Launching...
mvn spring-boot:run