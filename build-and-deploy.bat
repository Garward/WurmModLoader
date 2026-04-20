@echo off
REM WurmModLoader Build and Deploy (Windows)
REM Complete automation: build + deploy in one command

setlocal

set "PROJECT_DIR=%~dp0"
set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

cd /d "%PROJECT_DIR%"

echo ======================================================================
echo  WurmModLoader: Full Build ^& Deploy Automation
echo ======================================================================
echo.

echo [1/2] Building project...
echo.
call build.bat
if errorlevel 1 (
    echo.
    echo Build failed! Deployment aborted.
    exit /b 1
)

echo.
echo ======================================================================
echo.

echo [2/2] Deploying to server...
echo.
call deploy.bat
if errorlevel 1 (
    echo.
    echo Deployment failed!
    exit /b 1
)

echo.
echo ======================================================================
echo  Full Automation Complete!
echo ======================================================================
echo  Built and deployed successfully.
exit /b 0
