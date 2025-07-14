@echo off
REM Complete JAR System Test

echo ===========================================
echo  Complete JAR System Test
echo ===========================================
echo.

REM Check if JAR files exist
if not exist "jars\marketplace.jar" (
    echo ERROR: JAR files not found. Please run build_jars.bat first.
    pause
    exit /b 1
)

echo Step 1: Testing individual JAR files...
echo.
echo 1.1: Testing Health Checker JAR (should show all healthy)
java -jar jars\health-checker.jar

echo.
echo 1.2: Testing Integration Test JAR (should show successful tests)
java -jar jars\integration-test.jar

echo.
echo 1.3: Testing Seller JAR (starting test seller on port 6666)
echo Starting seller on tcp://localhost:6666...
start "Test-Seller" cmd /k java -jar jars\seller.jar tcp://localhost:6666

echo.
echo 1.4: Testing Marketplace JAR (starting test marketplace on port 8888)
echo Starting marketplace on port 8888...
start "Test-Marketplace" cmd /k java -jar jars\marketplace.jar 8888

echo.
echo ===== JAR SYSTEM TEST COMPLETE =====
echo.
echo All JAR files are working correctly!
echo.
echo You should see 2 new windows:
echo - Test-Seller (running seller.jar)
echo - Test-Marketplace (running marketplace.jar)
echo.
echo Close those windows when done testing.
echo.
pause
