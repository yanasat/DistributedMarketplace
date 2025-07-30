@echo off
REM Docker system startup script

echo =================================================
echo   Starting Distributed Marketplace (Docker)
echo =================================================

echo Step 1: Starting seller services...
docker-compose up -d seller1 seller2 seller3 seller4 seller5

echo Step 2: Waiting for sellers to be ready...
timeout /t 15 /nobreak

echo Step 3: Starting marketplace services...
docker-compose up -d marketplace1 marketplace2

echo Step 4: Checking service health...
docker-compose ps

echo.
echo ===== DOCKER SYSTEM STARTUP COMPLETE =====
echo.
echo Services running:
echo - 5 Seller containers (ports 5555-5559)
echo - 2 Marketplace containers (ports 7777-7778)
echo.
echo Available commands:
echo   docker-test.bat                      - Run integration tests
echo   docker-compose logs -f [service]     - View service logs
echo   docker-compose ps                    - Check service status
echo   docker-stop.bat                      - Stop all services
echo.
echo Example log commands:
echo   docker-compose logs -f seller1       - View seller1 logs
echo   docker-compose logs -f marketplace1  - View marketplace1 logs
echo.
