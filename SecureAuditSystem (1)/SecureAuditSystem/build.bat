@echo off
REM ============================================================
REM  build.bat - Compile and package (Windows)
REM ============================================================

set SRC_DIR=src
set OUT_DIR=out
set LIB_DIR=lib
set JAR_NAME=SecureAuditSystem.jar
set MAIN_CLASS=com.audit.menu.MainMenu

for %%f in (%LIB_DIR%\mysql-connector*.jar) do set MYSQL_JAR=%%f
if "%MYSQL_JAR%"=="" (
    echo [ERROR] MySQL JDBC connector not found in lib\
    echo   Download from: https://dev.mysql.com/downloads/connector/j/
    pause
    exit /b 1
)
echo [OK] Found: %MYSQL_JAR%

if exist %OUT_DIR% rmdir /s /q %OUT_DIR%
mkdir %OUT_DIR%

REM Collect all java files
dir /s /b %SRC_DIR%\*.java > sources.txt
for /f "tokens=*" %%i in (sources.txt) do echo "%%i" >> sources_quoted.txt
echo [INFO] Compiling sources...

javac -cp %MYSQL_JAR% -d %OUT_DIR% @sources_quoted.txt
if errorlevel 1 (
    echo [ERROR] Compilation failed.
    del sources.txt sources_quoted.txt
    pause
    exit /b 1
)
del sources.txt sources_quoted.txt
echo [OK] Compilation successful.

jar cfe %JAR_NAME% %MAIN_CLASS% -C %OUT_DIR% .
echo [OK] JAR created: %JAR_NAME%

echo.
echo To run:  java -cp "%JAR_NAME%;%MYSQL_JAR%" %MAIN_CLASS%
echo Or use:  run.bat
pause
