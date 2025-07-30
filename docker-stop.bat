@echo off
REM Docker system stop script

echo =================================================
echo   Stopping Distributed Marketplace (Docker)
echo =================================================

echo Step 1: Stopping all services...
docker-compose down

echo Step 2: Cleaning up containers...
docker-compose rm -f

echo Step 3: Showing system status...
docker-compose ps

echo.
echo ===== DOCKER SYSTEM STOPPED =====
echo.
echo To restart:
echo   docker-start.bat
echo.
echo To rebuild:
echo   docker-build.bat
echo.
