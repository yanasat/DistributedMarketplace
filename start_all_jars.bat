@echo off
REM Auto-start entire system using JAR files

echo =================================
echo  Distributed Marketplace System
echo        (JAR-based Execution)
echo =================================
echo.

REM Check if JAR files exist
if not exist "jars\marketplace.jar" (
    echo ERROR: JAR files not found. Please run build_jars.bat first.
    pause
    exit /b 1
)

echo Step 1: Starting all sellers using JAR files...
echo Starting 5 seller processes...

REM Start all sellers using JAR files
start "Seller-1" cmd /k java -jar jars\seller.jar tcp://localhost:5555
start "Seller-2" cmd /k java -jar jars\seller.jar tcp://localhost:5556
start "Seller-3" cmd /k java -jar jars\seller.jar tcp://localhost:5557
start "Seller-4" cmd /k java -jar jars\seller.jar tcp://localhost:5558
start "Seller-5" cmd /k java -jar jars\seller.jar tcp://localhost:5559

echo All 5 sellers started using JAR files!

echo.
echo Step 2: Waiting for sellers to initialize...
timeout /t 5 /nobreak > nul

echo Step 3: Starting all marketplaces using JAR files...
echo Starting 2 marketplace processes...

REM Start marketplaces using JAR files
start "Marketplace-1" cmd /k java -jar jars\marketplace.jar 7777
start "Marketplace-2" cmd /k java -jar jars\marketplace.jar 7778

echo All 2 marketplaces started using JAR files!

echo.
echo ===== SYSTEM STARTUP COMPLETE =====
echo.
echo You should now see 7 separate windows:
echo - 5 Seller windows (using seller.jar)
echo - 2 Marketplace windows (using marketplace.jar)
echo.
echo To test the system, run in a NEW window:
echo   java -jar jars\integration-test.jar
echo.
echo To check system health:
echo   java -jar jars\health-checker.jar
echo.
echo Press any key to close this startup window...
pause
