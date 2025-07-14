@echo off
echo =======================================
echo  Building Distributed Marketplace JARs
echo =======================================
echo.

echo Step 1: Compiling Java files...
call compile.bat
if errorlevel 1 (
    echo ERROR: Compilation failed
    pause
    exit /b 1
)

echo Step 2: Cleaning up old files...
if exist jars rmdir /s /q jars
if exist temp_lib rmdir /s /q temp_lib
if exist temp_manifest.txt del temp_manifest.txt

echo Step 3: Creating directories...
mkdir jars
mkdir temp_lib

echo Step 4: Extracting ZeroMQ library...
cd temp_lib
jar xf ..\lib\jeromq-0.5.2.jar
cd ..

echo Step 5: Building JAR files...
echo Main-Class: HealthChecker > temp_manifest.txt
echo. >> temp_manifest.txt
jar cfm jars\health-checker.jar temp_manifest.txt -C classes . -C temp_lib .

echo Main-Class: IntegrationTest > temp_manifest.txt
echo. >> temp_manifest.txt
jar cfm jars\integration-test.jar temp_manifest.txt -C classes . -C temp_lib .

echo Main-Class: SellerProcess > temp_manifest.txt
echo. >> temp_manifest.txt
jar cfm jars\seller.jar temp_manifest.txt -C classes . -C temp_lib .

echo Main-Class: MarketplaceProcess > temp_manifest.txt
echo. >> temp_manifest.txt
jar cfm jars\marketplace.jar temp_manifest.txt -C classes . -C temp_lib .

echo Step 6: Cleaning up temporary files...
del temp_manifest.txt
rmdir /s /q temp_lib

echo.
echo ===== JAR BUILD COMPLETE =====
echo.
echo Created JAR files:
echo - jars\health-checker.jar
echo - jars\integration-test.jar
echo - jars\seller.jar
echo - jars\marketplace.jar
echo.
echo Usage:
echo   java -jar jars\health-checker.jar
echo   java -jar jars\integration-test.jar
echo   java -jar jars\seller.jar [endpoint]
echo   java -jar jars\marketplace.jar [port]
echo.
pause
