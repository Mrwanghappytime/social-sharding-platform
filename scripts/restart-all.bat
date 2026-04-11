@echo off
setlocal enabledelayedexpansion

:: Social Platform - Stop, Build and Start All Services
:: Usage: restart-all.bat

set "PROJECT_DIR=%~dp0.."
cd /d "%PROJECT_DIR%"

echo ===========================================
echo Social Platform - Restart All Services
echo ===========================================
echo.

:: Stop all services by killing processes on ports
echo [1/3] Stopping all services...

for %%A in (gateway user-service post-service interaction-service relation-service notification-service file-service facade-service) do (
    echo   Stopping %%A...
)

:: Kill by port
for %%P in (8080 8081 8082 8083 8084 8085 8086 8087) do (
    for /f "tokens=5" %%L in ('netstat -ano ^| findstr :%%P ^| findstr LISTENING') do (
        taskkill /F /PID %%L >nul 2>&1
    )
)

echo.
echo [2/3] Building all modules...
call mvn clean install -DskipTests -q

if errorlevel 1 (
    echo Build failed!
    exit /b 1
)

echo.
echo [3/3] Starting all services...

:: JVM memory settings
set JVM_OPTS=-Xmx300m -Xms100m

:: Create logs directory if not exists
if not exist "%PROJECT_DIR%\logs" mkdir "%PROJECT_DIR%\logs"

:: Service startup: name|http_port|module|debug_port
set S1=gateway|8080|gateway|5005
set S2=user-service|8081|user-service|5006
set S3=post-service|8082|post-service|5007
set S4=interaction-service|8083|interaction-service|5008
set S5=relation-service|8084|relation-service|5009
set S6=notification-service|8085|notification-service|5010
set S7=file-service|8086|file-service|5011
set S8=facade-service|8087|facade-service|5012

for %%S in (S1 S2 S3 S4 S5 S6 S7 S8) do (
    for /f "tokens=1-4 delims=|" %%N in ('echo !%%S!') do (
        echo   Starting %%N on port %%O (debug: %%P)...
        cd /d "%PROJECT_DIR%\%%M"
        start "%%N" cmd /c "set MAVEN_OPTS=%JVM_OPTS% -agentlib:jdwp=transport=dt_socket^,server=y^,suspend=n^,address=%%P && mvn spring-boot:run > ""%PROJECT_DIR%\logs\%%N.log"" 2>&1"
    )
)

echo.
echo ===========================================
echo All services starting...
echo Logs directory: %PROJECT_DIR%\logs
echo ===========================================
echo.
echo Services:
echo   - gateway (port 8080, debug 5005)
echo   - user-service (port 8081, debug 5006)
echo   - post-service (port 8082, debug 5007)
echo   - interaction-service (port 8083, debug 5008)
echo   - relation-service (port 8084, debug 5009)
echo   - notification-service (port 8085, debug 5010)
echo   - file-service (port 8086, debug 5011)
echo   - facade-service (port 8087, debug 5012)
echo.
