@echo off
REM ============================================================
REM  run.bat - Run the Secure Audit System (Windows)
REM ============================================================

set JAR_NAME=SecureAuditSystem.jar
set MAIN_CLASS=com.audit.menu.MainMenu

for %%f in (lib\mysql-connector*.jar) do set MYSQL_JAR=%%f
if "%MYSQL_JAR%"=="" (
    echo [ERROR] MySQL JDBC connector not found in lib\
    pause
    exit /b 1
)

if not exist %JAR_NAME% (
    echo [ERROR] %JAR_NAME% not found. Run build.bat first.
    pause
    exit /b 1
)

java -cp "%JAR_NAME%;%MYSQL_JAR%" %MAIN_CLASS%
pause
