@echo off
REM WurmModLoader Smart Deploy Script (Windows)
REM Deploys framework and mods to Wurm server (only changed files)

setlocal enabledelayedexpansion

set "PROJECT_DIR=%~dp0"
set "PROJECT_DIR=%PROJECT_DIR:~0,-1%"

REM Server location: env var WURM_SERVER_DIR or Steam default
if defined WURM_SERVER_DIR (
    set "SERVER_DIR=%WURM_SERVER_DIR%"
) else (
    set "SERVER_DIR=C:\Program Files (x86)\Steam\steamapps\common\Wurm Unlimited Dedicated Server"
)

set /a COPIED=0
set /a SKIPPED=0
set /a ERRORS=0

echo ======================================================================
echo  WurmModLoader Smart Deploy
echo ======================================================================
echo.

REM Find latest distribution ZIP
set "DIST_ZIP="
for /f "delims=" %%f in ('dir /b /o-d "%PROJECT_DIR%\build\distributions\WurmModloader-Runtime-*.zip" 2^>nul') do (
    if not defined DIST_ZIP set "DIST_ZIP=%PROJECT_DIR%\build\distributions\%%f"
)

if not defined DIST_ZIP (
    echo [ERROR] Distribution ZIP not found.
    echo Expected location: %PROJECT_DIR%\build\distributions\
    echo Run build first:   build.bat
    exit /b 1
)

if not exist "%SERVER_DIR%" (
    echo [ERROR] Server directory not found: %SERVER_DIR%
    echo Set WURM_SERVER_DIR to override.
    exit /b 1
)

echo Distribution: %DIST_ZIP%
echo Server:       %SERVER_DIR%
echo.

REM Extract distribution to temp
set "TEMP_EXTRACT=%TEMP%\wurmmodloader-deploy-%RANDOM%"
mkdir "%TEMP_EXTRACT%" >nul 2>&1

echo Extracting distribution to temp...
powershell -NoProfile -Command "Expand-Archive -LiteralPath '%DIST_ZIP%' -DestinationPath '%TEMP_EXTRACT%' -Force"
if errorlevel 1 (
    echo [ERROR] Failed to extract distribution.
    exit /b 1
)
echo OK
echo.

REM Deploy framework JARs to server root
echo ======================================================================
echo  Deploying Framework JARs
echo ======================================================================
for %%f in ("%TEMP_EXTRACT%\*.jar") do (
    call :copy_if_changed "%%f" "%SERVER_DIR%\%%~nxf" "Framework: %%~nxf"
)
echo.

REM Seed HTTP config (non-destructive)
set "HTTP_CFG_SRC=%PROJECT_DIR%\docs\reference\wurmmodloader-http.properties.example"
set "HTTP_CFG_DEST=%SERVER_DIR%\config\wurmmodloader-http.properties"
if exist "%HTTP_CFG_SRC%" if not exist "%HTTP_CFG_DEST%" (
    if not exist "%SERVER_DIR%\config" mkdir "%SERVER_DIR%\config"
    copy /y "%HTTP_CFG_SRC%" "%HTTP_CFG_DEST%" >nul
    echo  [+] Seeded config\wurmmodloader-http.properties
    set /a COPIED+=1
)

set "VFIX_CFG_SRC=%PROJECT_DIR%\docs\reference\wurmmodloader-vanilla-fixes.properties.example"
set "VFIX_CFG_DEST=%SERVER_DIR%\config\wurmmodloader-vanilla-fixes.properties"
if exist "%VFIX_CFG_SRC%" if not exist "%VFIX_CFG_DEST%" (
    if not exist "%SERVER_DIR%\config" mkdir "%SERVER_DIR%\config"
    copy /y "%VFIX_CFG_SRC%" "%VFIX_CFG_DEST%" >nul
    echo  [+] Seeded config\wurmmodloader-vanilla-fixes.properties
    set /a COPIED+=1
)

REM Deploy mods
echo ======================================================================
echo  Deploying Mods
echo ======================================================================
for /d %%d in ("%PROJECT_DIR%\mods\*") do (
    set "MOD_NAME=%%~nxd"
    set "MOD_JAR=%%d\build\libs\!MOD_NAME!.jar"
    if exist "!MOD_JAR!" (
        if not exist "%SERVER_DIR%\mods\!MOD_NAME!" mkdir "%SERVER_DIR%\mods\!MOD_NAME!"
        call :copy_if_changed "!MOD_JAR!" "%SERVER_DIR%\mods\!MOD_NAME!\!MOD_NAME!.jar" "Mod: !MOD_NAME!"
        set "MOD_PROPS=%%d\src\dist\!MOD_NAME!.properties"
        if exist "!MOD_PROPS!" (
            call :copy_if_changed "!MOD_PROPS!" "%SERVER_DIR%\mods\!MOD_NAME!.properties" "Config: !MOD_NAME!.properties"
        )
    ) else (
        echo  [!] Mod: !MOD_NAME! - JAR not built (run build.bat^)
    )
)
echo.

REM Cleanup
echo Cleaning up...
rmdir /s /q "%TEMP_EXTRACT%"
echo.

echo ======================================================================
echo  Deployment Summary
echo ======================================================================
echo  Copied:    %COPIED% files
echo  Unchanged: %SKIPPED% files
if %ERRORS% gtr 0 (
    echo  Errors:    %ERRORS%
    echo.
    echo Deployment completed with errors.
    exit /b 1
)
echo.
echo Deployment complete!
echo.
echo Next steps:
echo   1. Start your Wurm server (wurmmodloader.bat)
echo   2. Check logs for mod loading
echo   3. Test your changes
exit /b 0

:copy_if_changed
REM %1=src %2=dest %3=desc
set "SRC=%~1"
set "DEST=%~2"
set "DESC=%~3"
if not exist "%SRC%" (
    echo  [X] %DESC% - source not found
    set /a ERRORS+=1
    goto :eof
)
if exist "%DEST%" (
    fc /b "%SRC%" "%DEST%" >nul 2>&1
    if not errorlevel 1 (
        echo  [.] %DESC% - unchanged
        set /a SKIPPED+=1
        goto :eof
    )
)
for %%P in ("%DEST%") do if not exist "%%~dpP" mkdir "%%~dpP" >nul 2>&1
copy /y "%SRC%" "%DEST%" >nul
if errorlevel 1 (
    echo  [X] %DESC% - copy failed
    set /a ERRORS+=1
) else (
    echo  [+] %DESC%
    set /a COPIED+=1
)
goto :eof
