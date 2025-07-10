@echo off
REM Compile all Java files in the project

echo Compiling all Java files...

REM Create classes directory if it doesn't exist
if not exist "classes" mkdir classes

REM Compile each file individually to avoid wildcard issues
echo Compiling core classes...
javac -cp "lib\jeromq-0.5.2.jar" -d classes src\messaging\MessageUtils.java
if %errorlevel% neq 0 goto :error

javac -cp "lib\jeromq-0.5.2.jar;classes" -d classes src\model\Order.java
if %errorlevel% neq 0 goto :error

javac -cp "lib\jeromq-0.5.2.jar;classes" -d classes src\seller\SellerStub.java
if %errorlevel% neq 0 goto :error

javac -cp "lib\jeromq-0.5.2.jar;classes" -d classes src\marketplace\Marketplace.java
if %errorlevel% neq 0 goto :error

echo Compiling main classes...
javac -cp "lib\jeromq-0.5.2.jar;classes" -d classes src\Main.java
if %errorlevel% neq 0 goto :error

javac -cp "lib\jeromq-0.5.2.jar;classes" -d classes src\MarketplaceProcess.java
if %errorlevel% neq 0 goto :error

javac -cp "lib\jeromq-0.5.2.jar;classes" -d classes src\SellerProcess.java
if %errorlevel% neq 0 goto :error

javac -cp "lib\jeromq-0.5.2.jar;classes" -d classes src\IntegrationTest.java
if %errorlevel% neq 0 goto :error

echo All files compiled successfully!
goto :end

:error
echo Compilation failed!
pause
exit /b 1

:end
