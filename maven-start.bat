@echo off
REM Maven system startup script for Windows
echo ===============================================
echo   Starting Distributed Marketplace (Maven)
echo ===============================================

echo Step 1: Starting 5 seller processes...
start "Seller-1" cmd /k mvn exec:java -Pseller -Dexec.args="tcp://localhost:5555"
start "Seller-2" cmd /k mvn exec:java -Pseller -Dexec.args="tcp://localhost:5556"
start "Seller-3" cmd /k mvn exec:java -Pseller -Dexec.args="tcp://localhost:5557"
start "Seller-4" cmd /k mvn exec:java -Pseller -Dexec.args="tcp://localhost:5558"
start "Seller-5" cmd /k mvn exec:java -Pseller -Dexec.args="tcp://localhost:5559"

echo All 5 sellers started!

echo.
echo Step 2: Waiting for sellers to initialize...
timeout /t 8 /nobreak > nul

echo Step 3: Starting 2 marketplace processes...
start "Marketplace-1" cmd /k mvn exec:java -Pmarketplace -Dexec.args="7777"
start "Marketplace-2" cmd /k mvn exec:java -Pmarketplace -Dexec.args="7778"

echo All 2 marketplaces started!

echo.
echo ===== MAVEN SYSTEM STARTUP COMPLETE =====
echo.
echo You should now see 7 separate windows:
echo - 5 Seller windows (Maven profiles)
echo - 2 Marketplace windows (Maven profiles)
echo.
echo To test the system:
echo   mvn exec:java -Pintegration-test
echo.
echo To check system health:
echo   mvn exec:java -Phealth-check
echo.
echo Press any key to close this startup window...
pause
