@echo off
REM Script to run the integration test

echo Running Integration Test...
echo.
echo Make sure you have started the sellers and marketplaces first!
echo (Run start_all.bat in another window)
echo.

REM Wait a moment for user to read
timeout /t 3 /nobreak > nul

REM Run the integration test
java -cp "lib/jeromq-0.5.2.jar;classes" IntegrationTest

echo.
echo Integration test completed.
pause
