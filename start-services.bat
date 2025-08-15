@echo off
echo Starting Log Aggregation Platform...

REM Set Maven path for this session
set "MAVEN_HOME=C:\Program Files\apache-maven-3.9.5"
set "PATH=%MAVEN_HOME%\bin;%PATH%"

echo Verifying Maven installation...
mvn --version
if %ERRORLEVEL% neq 0 (
    echo ERROR: Maven not found. Please ensure Maven is installed.
    pause
    exit /b 1
)

echo Starting Log Agent...
cd log-agent
start "Log Agent" cmd /k "mvn spring-boot:run"
cd ..

timeout /t 10 /nobreak >nul

echo Starting Log Receiver...
cd log-receiver
start "Log Receiver" cmd /k "mvn spring-boot:run"
cd ..

timeout /t 10 /nobreak >nul

echo Starting Log Processor...
cd log-processor
start "Log Processor" cmd /k "mvn spring-boot:run"
cd ..

timeout /t 10 /nobreak >nul

echo Starting Log Dashboard...
cd log-dashboard
start "Log Dashboard" cmd /k "mvn spring-boot:run"
cd ..

echo.
echo All services are starting in separate windows...
echo Access the dashboard at: http://localhost:8080/dashboard
echo.
pause
