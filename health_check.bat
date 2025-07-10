@echo off
REM Health checker for distributed marketplace system

echo Running system health check...
echo.

REM Compile first to ensure we have the latest health checker
echo Compiling health checker...
javac -cp "lib\jeromq-0.5.2.jar;classes" -d classes src\HealthChecker.java
if %errorlevel% neq 0 (
    echo ERROR: Health checker compilation failed
    pause
    exit /b 1
)

echo.
echo Checking system health...
java -cp "lib\jeromq-0.5.2.jar;classes" HealthChecker

echo.
pause
