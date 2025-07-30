@echo off
REM Docker build script for Distributed Marketplace

echo =================================================
echo   Building Distributed Marketplace Docker Images
echo =================================================

REM Create docker directories if they don't exist
if not exist "docker\scripts" mkdir docker\scripts

echo Step 1: Building Docker image...
docker build -t distributed-marketplace:latest .

if %ERRORLEVEL% equ 0 (
    echo ✅ Docker image built successfully!
    
    echo.
    echo Step 2: Verifying image...
    docker images | findstr distributed-marketplace
    
    echo.
    echo ===== DOCKER BUILD COMPLETE =====
    echo.
    echo Available commands:
    echo   docker-start.bat               - Start all services
    echo   docker-test.bat                - Run integration tests
    echo   docker-stop.bat                - Stop all services
    echo   docker-compose logs -f         - View logs
    echo.
) else (
    echo ❌ Docker build failed!
    exit /b 1
)
