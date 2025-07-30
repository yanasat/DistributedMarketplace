@echo off
REM Component startup script for Docker containers

if "%1"=="" (
    echo Usage: start-component.bat [component-name] [config-file]
    echo Example: start-component.bat seller1 config/seller1.yaml
    exit /b 1
)

set COMPONENT=%1
set CONFIG_FILE=%2

if "%CONFIG_FILE%"=="" (
    set CONFIG_FILE=config/%COMPONENT%.yaml
)

echo Starting component: %COMPONENT%
echo Using config: %CONFIG_FILE%

REM Start the specific component
docker-compose up -d %COMPONENT%

REM Check if it started successfully
docker-compose ps %COMPONENT%

echo Component %COMPONENT% startup complete.
