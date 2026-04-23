@echo off
REM WurmModLoader - one-button new-world setup (Windows).
REM
REM Creates a clean world folder (sqlite DBs scrubbed of Adventure's hardcoded
REM villages/spawn), optionally drops in a custom map, and configures the
REM world seeder to paint a starter town at the landmass center on first boot.
REM
REM Usage (interactive):
REM     wurmmodloader-create-world.bat
REM
REM Usage (non-interactive):
REM     wurmmodloader-create-world.bat --name Riverweave --maps default --starter-town yes
REM
REM Flags mirror the Linux script: --name, --maps, --starter-town,
REM --template, --server, --force, --yes, --help.

setlocal EnableDelayedExpansion

set "WORLD_NAME="
set "MAPS_SOURCE="
set "STARTER_TOWN="
set "MODE="
set "TEMPLATE=Adventure"
set "SERVER_DIR=%~dp0"
set "SERVER_DIR=%SERVER_DIR:~0,-1%"
set "FORCE=0"
set "ACCEPT_DEFAULTS=0"

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="--name"          ( set "WORLD_NAME=%~2"   & shift & shift & goto parse_args )
if /I "%~1"=="--maps"          ( set "MAPS_SOURCE=%~2"  & shift & shift & goto parse_args )
if /I "%~1"=="--starter-town"  ( set "STARTER_TOWN=%~2" & shift & shift & goto parse_args )
if /I "%~1"=="--mode"          ( set "MODE=%~2"         & shift & shift & goto parse_args )
if /I "%~1"=="--template"      ( set "TEMPLATE=%~2"     & shift & shift & goto parse_args )
if /I "%~1"=="--server"        ( set "SERVER_DIR=%~2"   & shift & shift & goto parse_args )
if /I "%~1"=="--force"         ( set "FORCE=1"          & shift & goto parse_args )
if /I "%~1"=="--yes"           ( set "ACCEPT_DEFAULTS=1" & shift & goto parse_args )
if /I "%~1"=="--help"          ( goto show_help )
if /I "%~1"=="-h"              ( goto show_help )
echo ERROR: unknown argument: %~1
exit /b 1
:args_done

if not exist "%SERVER_DIR%\wurmmodloader.bat" (
    echo ERROR: doesn't look like a server root ^(no wurmmodloader.bat^): %SERVER_DIR%
    exit /b 1
)

set "REBUILD_SCRIPT=%SERVER_DIR%\wurmmodloader-rebuild-dbs.bat"
if not exist "%REBUILD_SCRIPT%" (
    echo ERROR: rebuild-dbs script missing: %REBUILD_SCRIPT%
    exit /b 1
)

REM ---------- prompts ----------
if "%WORLD_NAME%"=="" (
    if "%ACCEPT_DEFAULTS%"=="1" ( set "WORLD_NAME=Riverweave" ) else (
        set /p "WORLD_NAME=New world folder name [Riverweave]: "
        if "!WORLD_NAME!"=="" set "WORLD_NAME=Riverweave"
    )
)

if "%WORLD_NAME%"=="" ( echo ERROR: world name is required & exit /b 1 )

set "TEMPLATE_DIR=%SERVER_DIR%\%TEMPLATE%"
if not exist "%TEMPLATE_DIR%\" (
    echo ERROR: template world not found: %TEMPLATE_DIR%
    exit /b 1
)

REM If a matching map pack lives under Maps\<WorldName>\, offer it as default.
set "MAPS_DEFAULT=default"
if exist "%SERVER_DIR%\Maps\%WORLD_NAME%\top_layer.map" set "MAPS_DEFAULT=%SERVER_DIR%\Maps\%WORLD_NAME%"
if exist "%SERVER_DIR%\Maps\%WORLD_NAME%\rock_layer.map" set "MAPS_DEFAULT=%SERVER_DIR%\Maps\%WORLD_NAME%"

if "%MAPS_SOURCE%"=="" (
    if "%ACCEPT_DEFAULTS%"=="1" ( set "MAPS_SOURCE=%MAPS_DEFAULT%" ) else (
        set /p "MAPS_SOURCE=Maps: 'default' to clone %TEMPLATE% terrain, a directory path, or a pack name under Maps\ [%MAPS_DEFAULT%]: "
        if "!MAPS_SOURCE!"=="" set "MAPS_SOURCE=%MAPS_DEFAULT%"
    )
)
REM Resolve bare names (e.g. "Riverweave") against Maps\<name>\.
if /I not "%MAPS_SOURCE%"=="default" (
    if not exist "!MAPS_SOURCE!\" (
        if exist "%SERVER_DIR%\Maps\!MAPS_SOURCE!\" (
            echo ^>^>^> Resolving '!MAPS_SOURCE!' -^> %SERVER_DIR%\Maps\!MAPS_SOURCE!.
            set "MAPS_SOURCE=%SERVER_DIR%\Maps\!MAPS_SOURCE!"
        )
    )
)

if "%STARTER_TOWN%"=="" (
    if "%ACCEPT_DEFAULTS%"=="1" ( set "STARTER_TOWN=yes" ) else (
        set /p "STARTER_TOWN=Seed Winkshir starter town at the landmass center? (yes/no) [yes]: "
        if "!STARTER_TOWN!"=="" set "STARTER_TOWN=yes"
    )
)

set "ST_LOWER=%STARTER_TOWN%"
if /I "%ST_LOWER%"=="y"     set "STARTER_TOWN=yes"
if /I "%ST_LOWER%"=="true"  set "STARTER_TOWN=yes"
if /I "%ST_LOWER%"=="1"     set "STARTER_TOWN=yes"
if /I "%ST_LOWER%"=="n"     set "STARTER_TOWN=no"
if /I "%ST_LOWER%"=="false" set "STARTER_TOWN=no"
if /I "%ST_LOWER%"=="0"     set "STARTER_TOWN=no"

if /I not "%STARTER_TOWN%"=="yes" if /I not "%STARTER_TOWN%"=="no" (
    echo ERROR: invalid --starter-town value: %STARTER_TOWN%
    exit /b 1
)

if "%MODE%"=="" (
    if "%ACCEPT_DEFAULTS%"=="1" ( set "MODE=pve" ) else (
        set /p "MODE=Server mode: pve ^(peaceful home^) or pvp ^(Chaos-style^) [pve]: "
        if "!MODE!"=="" set "MODE=pve"
    )
)
if /I "%MODE%"=="pve" ( set "MODE=pve" ) else if /I "%MODE%"=="pvp" ( set "MODE=pvp" ) else (
    echo ERROR: invalid --mode value: %MODE% ^(expected pve/pvp^)
    exit /b 1
)

REM ---------- target prep ----------
set "WORLD_DIR=%SERVER_DIR%\%WORLD_NAME%"
if exist "%WORLD_DIR%\" (
    if "%FORCE%"=="1" (
        echo ^>^>^> Removing existing %WORLD_DIR% ^(--force^).
        rmdir /s /q "%WORLD_DIR%"
    ) else (
        echo ERROR: world already exists: %WORLD_DIR% ^(pass --force to overwrite^)
        exit /b 1
    )
)

mkdir "%WORLD_DIR%\sqlite" 2>nul
mkdir "%WORLD_DIR%\Logs" 2>nul

REM WU uses an empty 'gamedir' marker file to identify world folders in the
REM launcher. Without it, Folders.setCurrent() NPEs on start=<WorldName>.
type nul > "%WORLD_DIR%\gamedir"

REM ---------- sqlite scaffold ----------
echo ^>^>^> Copying sqlite scaffold from %TEMPLATE%\sqlite\.
for %%F in ("%TEMPLATE_DIR%\sqlite\*") do (
    set "FNAME=%%~nxF"
    set "SKIP=0"
    rem Keep .db files — they carry the migrated schema Wurm expects.
    if /I "%%~xF"==".db-wal" set "SKIP=1"
    if /I "%%~xF"==".db-shm" set "SKIP=1"
    if "!SKIP!"=="0" copy /y "%%F" "%WORLD_DIR%\sqlite\!FNAME!" >nul
)

REM ---------- wurm.ini ----------
if exist "%TEMPLATE_DIR%\wurm.ini" copy /y "%TEMPLATE_DIR%\wurm.ini" "%WORLD_DIR%\wurm.ini" >nul

REM ---------- maps ----------
REM Game data (required for terrain) + client-side visuals (optional — livemap
REM and the launcher preview ship these alongside the .map files).
set "MAP_FILES=top_layer.map rock_layer.map map_cave.map flags.map resources.map protectedTiles.bmap map_actions.act"
set "MAP_EXTRAS=heightmap.png biomes.png cave.png map.png topography.png"
if /I "%MAPS_SOURCE%"=="default" (
    echo ^>^>^> Cloning map files from %TEMPLATE%\.
    for %%M in (%MAP_FILES% %MAP_EXTRAS%) do (
        if exist "%TEMPLATE_DIR%\%%M" copy /y "%TEMPLATE_DIR%\%%M" "%WORLD_DIR%\%%M" >nul
    )
) else (
    if not exist "%MAPS_SOURCE%\" (
        echo ERROR: custom map source not found: %MAPS_SOURCE%
        exit /b 1
    )
    echo ^>^>^> Copying custom map files from %MAPS_SOURCE%.
    set "FOUND=0"
    for %%M in (%MAP_FILES%) do (
        if exist "%MAPS_SOURCE%\%%M" (
            copy /y "%MAPS_SOURCE%\%%M" "%WORLD_DIR%\%%M" >nul
            set /a FOUND+=1
        )
    )
    for %%M in (%MAP_EXTRAS%) do (
        if exist "%MAPS_SOURCE%\%%M" copy /y "%MAPS_SOURCE%\%%M" "%WORLD_DIR%\%%M" >nul
    )
    if "!FOUND!"=="0" (
        echo ERROR: no map files found in %MAPS_SOURCE%
        exit /b 1
    )
    if not exist "%WORLD_DIR%\protectedTiles.bmap" (
        if exist "%TEMPLATE_DIR%\protectedTiles.bmap" copy /y "%TEMPLATE_DIR%\protectedTiles.bmap" "%WORLD_DIR%\protectedTiles.bmap" >nul
    )
)

REM ---------- rebuild dbs ----------
echo ^>^>^> Rebuilding ^& scrubbing databases via %REBUILD_SCRIPT%.
call "%REBUILD_SCRIPT%" -noconfirm -nopause "%WORLD_DIR%\sqlite" >nul
if errorlevel 1 (
    echo ERROR: rebuild-dbs failed - inspect %WORLD_DIR%\sqlite\ and rerun manually.
    exit /b 1
)

REM ---------- server mode override ----------
REM rebuild-dbs forces a peaceful Freedom home (HOMESERVER=1, KINGDOM=4, PVP=0).
REM Kingdom stays Freedom in both modes; pvp only flips HOMESERVER + PVP.
if /I "%MODE%"=="pvp" (
    echo ^>^>^> Setting SERVERS to Chaos-style ^(HOMESERVER=0, PVP=1^).
    set "SQLITE_BIN=%SERVER_DIR%\nativelibs\sqlite\sqlite3.exe"
    if not exist "!SQLITE_BIN!" set "SQLITE_BIN=sqlite3"
    "!SQLITE_BIN!" "%WORLD_DIR%\sqlite\wurmlogin.db" "UPDATE SERVERS SET HOMESERVER=0, PVP=1;"
)

REM ---------- world-seed config ----------
set "SEED_CONFIG=%SERVER_DIR%\config\wurmmodloader-world-seed.yaml"
if not exist "%SERVER_DIR%\config\" mkdir "%SERVER_DIR%\config"

echo ^>^>^> Writing %SEED_CONFIG%.
(
    echo # Auto-generated by wurmmodloader-create-world.bat
    echo # World: %WORLD_NAME%
    echo enabled: true
    echo force: false
    echo strategy: center
    echo centerWaterBuffer: 20
    if /I "%STARTER_TOWN%"=="yes" (
        echo importStarterTown: true
        echo starterTownSnapshot: winkshir
    ) else (
        echo importStarterTown: false
    )
    echo createVillage: true
    echo centerTownName: Freedom Landing
    echo despawnFounderNpc: true
) > "%SEED_CONFIG%"

echo.
echo World '%WORLD_NAME%' ready.
echo   Folder:       %WORLD_DIR%
echo   Map source:   %MAPS_SOURCE%
echo   Starter town: %STARTER_TOWN%
echo   Mode:         %MODE%
echo   Seed config:  %SEED_CONFIG%
echo.
echo Launch the server with:
echo     wurmmodloader.bat start=%WORLD_NAME%
echo.
exit /b 0

:show_help
echo.
echo wurmmodloader-create-world.bat
echo.
echo Flags:
echo   --name ^<WorldName^>         Folder name under the server root
echo   --maps ^<dir^|default^>       Custom map dir or "default" to clone the template
echo   --starter-town yes^|no      Seed Winkshir at the map center
echo   --mode pve^|pvp             pve = Jenn-Kellon home (default), pvp = Chaos-style
echo   --template ^<WorldName^>     Template world to clone ^(default: Adventure^)
echo   --server ^<dir^>             Server root ^(default: script's own dir^)
echo   --force                    Overwrite an existing world folder of the same name
echo   --yes                      Accept all defaults, no prompts
echo.
exit /b 0
