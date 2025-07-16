@echo off
REM Maven build script for Windows
echo ===============================================
echo   Building Distributed Marketplace (Maven)
echo ===============================================

echo Step 1: Compiling with Maven...
call mvn clean compile

if %errorlevel% neq 0 (
    echo ERROR: Maven compilation failed
    pause
    exit /b 1
)

echo Step 2: Running tests...
call mvn test

echo Step 3: Building JAR packages...
call mvn package

echo.
echo ===== MAVEN BUILD COMPLETE =====
echo.
echo Available commands:
echo   mvn exec:java                          - Run integration test
echo   mvn exec:java -Phealth-check          - Run health checker
echo   mvn exec:java -Pseller                 - Run seller process
echo   mvn exec:java -Pmarketplace            - Run marketplace process
echo.
pause
