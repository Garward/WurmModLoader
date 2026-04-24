@echo off
REM WurmModLoader launcher script

cd /d "%~dp0"

setlocal enabledelayedexpansion

REM Uber jar bundles api/core/modsupport/legacy/cli + javassist/gson/snakeyaml.
call :resolveJar "wurmmodloader-*.jar" UBER_JAR || goto :eof

REM =============================
REM Interactive menu (no args passed)
REM =============================
REM Without start=<World> vanilla Wurm silently pops the Steam GUI, which gives
REM no hint that modded boots require start=<folder>. This menu is the friendly
REM default; power users pass start=<World> and skip straight to launch.
set "ARGS=%*"
if "%ARGS%"=="" goto :menu
goto :launch

:menu
echo.
echo ===============================================================
for %%V in ("%UBER_JAR%") do set "UBER_NAME=%%~nV"
set "UBER_VER=!UBER_NAME:wurmmodloader-=!"
echo   WurmModLoader -- !UBER_VER!
echo ===============================================================
echo.
echo   What would you like to do?
echo.
echo     1^) Start an existing world
echo     2^) Create a new world          (wurmmodloader-create-world)
echo     3^) Rebuild world databases     (wurmmodloader-rebuild-dbs)
echo     4^) Launch vanilla Wurm GUI     (no modloader -- advanced)
echo     5^) Exit
echo.
echo   Tip: skip this menu with:  wurmmodloader.bat start=^<WorldName^>
echo.
set /p CHOICE="Choice [1-5]: "

if "%CHOICE%"=="1" goto :pickWorld
if "%CHOICE%"=="2" (
    call wurmmodloader-create-world.bat
    goto :eof
)
if "%CHOICE%"=="3" (
    call wurmmodloader-rebuild-dbs.bat
    goto :eof
)
if "%CHOICE%"=="4" (
    echo.
    echo Launching vanilla Steam GUI ^(modloader disabled^)...
    goto :launch
)
if "%CHOICE%"=="5" goto :eof
echo Invalid choice.
goto :menu

:pickWorld
set "WORLD_COUNT=0"
echo.
echo Available worlds:
for /d %%D in (*) do (
    if exist "%%D\wurm.ini" (
        set /a WORLD_COUNT+=1
        set "WORLD_!WORLD_COUNT!=%%D"
        echo    !WORLD_COUNT!^) %%D
    )
)
if %WORLD_COUNT%==0 (
    echo    (none found -- run option 2 first to create one)
    echo.
    goto :menu
)
echo.
set /p WPICK="Pick a world [1-%WORLD_COUNT%] (or blank to go back): "
if "%WPICK%"=="" goto :menu
call set "CHOSEN=%%WORLD_%WPICK%%%"
if "%CHOSEN%"=="" (
    echo Invalid selection.
    goto :menu
)
set "ARGS=start=%CHOSEN%"
goto :launch

:launch
set CP=%UBER_JAR%
set CP=%CP%;server.jar
set CP=%CP%;common.jar

REM Launch modloader
java -cp "%CP%" -Djava.util.logging.config.file=logging.properties ^
    -Xmn512M -Xms1g -Xmx4g -XX:+OptimizeStringConcat ^
    com.garward.wurmmodloader.serverlauncher.ServerLauncher %ARGS%

goto :eof

:resolveJar
for %%j in (%~1) do (
    set "%~2=%%j"
    goto :resolveJarSuccess
)
echo ERROR: Unable to find jar matching pattern %~1
exit /b 1
:resolveJarSuccess
exit /b 0
