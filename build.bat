@echo off
REM WurmModLoader Build Script (Windows)
REM Runs clean build and creates distribution

setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

echo ======================================================================
echo  WurmModLoader Build Script
echo ======================================================================
echo.

cd /d "%PROJECT_DIR%"

echo Project Directory: %PROJECT_DIR%
echo Gradle Version:
call gradlew.bat --version | findstr /R "Gradle JVM"
echo.

echo ======================================================================
echo  Running: gradlew.bat clean build dist
echo ======================================================================
echo.

call gradlew.bat clean build dist
if errorlevel 1 (
    echo.
    echo ======================================================================
    echo  Build Failed!
    echo ======================================================================
    echo  Check the error output above for details.
    exit /b 1
)

echo.
echo ======================================================================
echo  Build Successful!
echo ======================================================================
echo.

echo Distribution:
dir /b build\distributions\*.zip 2>nul
echo.

echo Framework JARs:
for /r wurmmodloader-* %%f in (build\libs\*.jar) do (
    echo %%f | findstr /v "sources javadoc" >nul && echo %%f
)
echo.

echo Mod JARs:
for /r mods %%f in (build\libs\*.jar) do (
    echo %%f | findstr /v "sources javadoc" >nul && echo %%f
)
echo.

echo Ready to deploy! Run: deploy.bat
echo.
exit /b 0
