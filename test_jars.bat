@echo off
REM Quick test using JAR files

echo =================================
echo  Quick System Test (JAR-based)
echo =================================
echo.

REM Check if JAR files exist
if not exist "jars\marketplace.jar" (
    echo ERROR: JAR files not found. Please run build_jars.bat first.
    pause
    exit /b 1
)

echo Step 1: Health Check...
java -jar jars\health-checker.jar

echo.
echo Step 2: Integration Test...
java -jar jars\integration-test.jar

echo.
echo Test complete!
pause
