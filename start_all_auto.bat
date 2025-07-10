@echo off
REM Auto-start entire system without user interaction

echo =================================
echo  Distributed Marketplace System
echo =================================
echo.

echo Step 1: Compiling all Java files...
call compile.bat
if %errorlevel% neq 0 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo Step 2: Starting all sellers...
echo Starting 5 seller processes automatically...

REM Start all sellers automatically
start "Seller-1" cmd /k java -cp "lib\jeromq-0.5.2.jar;classes" SellerProcess tcp://localhost:5555
start "Seller-2" cmd /k java -cp "lib\jeromq-0.5.2.jar;classes" SellerProcess tcp://localhost:5556
start "Seller-3" cmd /k java -cp "lib\jeromq-0.5.2.jar;classes" SellerProcess tcp://localhost:5557
start "Seller-4" cmd /k java -cp "lib\jeromq-0.5.2.jar;classes" SellerProcess tcp://localhost:5558
start "Seller-5" cmd /k java -cp "lib\jeromq-0.5.2.jar;classes" SellerProcess tcp://localhost:5559

echo All 5 sellers started in separate windows!

echo.
echo Step 3: Waiting for sellers to initialize...
timeout /t 5 /nobreak > nul

echo Step 4: Starting all marketplaces...
echo Starting 2 marketplace processes automatically...

REM Start marketplaces automatically  
start "Marketplace-1" cmd /k java -cp "lib\jeromq-0.5.2.jar;classes" MarketplaceProcess 7777
start "Marketplace-2" cmd /k java -cp "lib\jeromq-0.5.2.jar;classes" MarketplaceProcess 7778

echo All 2 marketplaces started in separate windows!

echo.
echo ===== SYSTEM STARTUP COMPLETE =====
echo.
echo You should now see 7 separate windows:
echo - 5 Seller windows (ports 5555-5559)
echo - 2 Marketplace windows (ports 7777-7778)
echo.
echo To test the system, run in a NEW window:
echo   .\run_integration_test.bat
echo.
echo Press any key to close this startup window...
pause
