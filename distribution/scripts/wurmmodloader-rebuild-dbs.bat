@echo off
rem wurmmodloader-rebuild-dbs.bat
rem
rem Windows counterpart to wurmmodloader-rebuild-dbs.sh. Replaces vanilla's
rem rebuild-dbs.bat with a variant that scrubs Adventure-specific hardcoded
rem villages / spawn points / HotA zones / recruitment boards so the
rem WurmModLoader WorldSeedBootstrap can seed a sensible starter town on
rem first boot.
rem
rem Usage:
rem   cd <ServerRoot>\<WorldName>\sqlite
rem   <ServerRoot>\wurmmodloader-rebuild-dbs.bat
rem
rem   REM or from anywhere, pointing at the sqlite dir:
rem   <ServerRoot>\wurmmodloader-rebuild-dbs.bat <ServerRoot>\<WorldName>\sqlite
rem
rem   REM or from the server root with just the world name:
rem   cd <ServerRoot>
rem   wurmmodloader-rebuild-dbs.bat <WorldName>
rem
rem Flags:
rem   -noconfirm   skip the "are you sure" prompt
rem   -nopause     skip the trailing pause

setlocal EnableDelayedExpansion

set "CONFIRM=1"
set "PAUSE_AT_END=1"
set "TARGET="

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="-noconfirm" ( set "CONFIRM=0" & shift & goto parse_args )
if /I "%~1"=="-nopause"   ( set "PAUSE_AT_END=0" & shift & goto parse_args )
if /I "%~1"=="--help"     goto show_help
if /I "%~1"=="-h"         goto show_help
if "%~1:~0,1%"=="-" (
    echo Unknown flag: %~1 >&2
    exit /b 2
)
if defined TARGET (
    echo Too many positional args ^(got extra: %~1^) >&2
    exit /b 2
)
set "TARGET=%~1"
shift
goto parse_args

:show_help
echo Usage: wurmmodloader-rebuild-dbs.bat [-noconfirm] [-nopause] [^<WorldName^>^|^<path-to-sqlite^>]
exit /b 0

:args_done

rem --- Resolve sqlite directory ------------------------------------------------
set "SQLITE_DIR="
if defined TARGET (
    if exist "%TARGET%\wurmzones.sql" (
        pushd "%TARGET%" & set "SQLITE_DIR=!CD!" & popd
    ) else if exist "%TARGET%\sqlite\wurmzones.sql" (
        pushd "%TARGET%\sqlite" & set "SQLITE_DIR=!CD!" & popd
    ) else if exist ".\%TARGET%\sqlite\wurmzones.sql" (
        pushd ".\%TARGET%\sqlite" & set "SQLITE_DIR=!CD!" & popd
    )
) else (
    if exist ".\wurmzones.sql" (
        set "SQLITE_DIR=%CD%"
    )
)

if not defined SQLITE_DIR (
    echo ERROR: Could not find a Wurm sqlite directory. >&2
    echo Expected a directory containing wurmzones.sql. >&2
    echo Run from inside ^<WorldName^>\sqlite\ or pass the world name as an argument. >&2
    exit /b 1
)

rem --- Pick a usable sqlite3.exe ----------------------------------------------
set "SQLITE_CMD="
if exist "%SQLITE_DIR%\sqlite3.exe" (
    set "SQLITE_CMD=%SQLITE_DIR%\sqlite3.exe"
) else (
    rem Check server root (parent of the world dir).
    for %%I in ("%SQLITE_DIR%\..\..") do set "SERVER_ROOT=%%~fI"
    if exist "!SERVER_ROOT!\sqlite3.exe" (
        set "SQLITE_CMD=!SERVER_ROOT!\sqlite3.exe"
    ) else (
        for /f "delims=" %%S in ('where sqlite3 2^>nul') do (
            if not defined SQLITE_CMD set "SQLITE_CMD=%%S"
        )
    )
)

if not defined SQLITE_CMD (
    echo ERROR: no sqlite3.exe found ^(checked %SQLITE_DIR%\, server root, PATH^). >&2
    echo Drop a sqlite3.exe in the server root or install one reachable via PATH. >&2
    exit /b 1
)

echo ======================================================================
echo WurmModLoader Clean Rebuild
echo ======================================================================
echo Target : %SQLITE_DIR%
echo sqlite : %SQLITE_CMD%
echo.

if "%CONFIRM%"=="1" (
    echo This will DELETE and rebuild all databases in:
    echo    %SQLITE_DIR%
    echo Adventure-specific hardcoded villages / spawns / zones will be scrubbed.
    set /p "ANSWER=Proceed? (y/[n]) "
    if /I not "!ANSWER!"=="y" (
        echo Aborted.
        exit /b 0
    )
)

set "DB_NAMES=wurmcreatures wurmdeities wurmeconomy wurmitems wurmlogin wurmlogs wurmplayers wurmtemplates wurmzones"

rem DBs whose *data* we scrub. The shipped .db files carry the migrated schema
rem Wurm expects (ITEMDATA.EXTRA1/EXTRA2, RECIPES* tables, PLAYERS nutrition
rem cols). The wurm*.sql scaffolds are stale; dropping+recreating from them
rem yields a DB Wurm treats as broken. We keep template .db files intact and
rem only DELETE FROM game-state tables. Bootstrap DBs retain their rows so
rem Wurm has item templates, pantheon, SERVERS row, etc.
set "SCRUB_DATA= wurmcreatures wurmeconomy wurmitems wurmplayers wurmzones "

rem --- Phase 1: verify template .db files are present -------------------------
set "MISSING=0"
for %%D in (%DB_NAMES%) do (
    if not exist "%SQLITE_DIR%\%%D.db" (
        echo Missing: %SQLITE_DIR%\%%D.db ^(template .db not copied in?^) >&2
        set "MISSING=1"
    )
)
if "%MISSING%"=="1" exit /b 1

rem --- Phase 2: scrub rows, preserve schema -----------------------------------
echo [1/2] Scrubbing Adventure row data from template databases...
for %%D in (%DB_NAMES%) do (
    set "TARGET_DB=%SQLITE_DIR%\%%D.db"
    rem Drop transient WAL/SHM companions so next open is clean.
    if exist "!TARGET_DB!-wal" del /q "!TARGET_DB!-wal"
    if exist "!TARGET_DB!-shm" del /q "!TARGET_DB!-shm"
    set "DO_SCRUB=0"
    echo %SCRUB_DATA% | findstr /C:" %%D " >nul
    if not errorlevel 1 set "DO_SCRUB=1"
    if "!DO_SCRUB!"=="1" (
        set "TBL_LIST=%TEMP%\wurmmodloader-tables-%%D.txt"
        set "SQL_FILE=%TEMP%\wurmmodloader-scrub-%%D.sql"
        "%SQLITE_CMD%" "!TARGET_DB!" "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%%' AND name <> 'SCHEMA_VERSION';" > "!TBL_LIST!"
        > "!SQL_FILE!" echo PRAGMA foreign_keys=OFF;
        >> "!SQL_FILE!" echo BEGIN;
        for /f "usebackq delims=" %%T in ("!TBL_LIST!") do >> "!SQL_FILE!" echo DELETE FROM "%%T";
        >> "!SQL_FILE!" echo COMMIT;
        >> "!SQL_FILE!" echo VACUUM;
        "%SQLITE_CMD%" "!TARGET_DB!" < "!SQL_FILE!" >nul
        del /q "!TBL_LIST!" "!SQL_FILE!" 2>nul
        echo   OK  %%D.db ^(emptied user tables; schema preserved^)
    ) else (
        echo   OK  %%D.db ^(bootstrap data retained^)
    )
)

rem --- Phase 3: normalize bootstrap values ------------------------------------
echo.
echo [2/2] Normalizing bootstrap values for a fresh world...

rem World folder name = parent directory of sqlite\. Inserted SERVERS row
rem still carries Adventure's NAME/MAPNAME ('Heavenord'); rename both to match.
for %%I in ("%SQLITE_DIR%\..") do set "WORLD_NAME=%%~nxI"

for /f %%C in ('"%SQLITE_CMD%" "%SQLITE_DIR%\wurmlogin.db" "SELECT COUNT(*) FROM HISTORY;"') do set "HISTORY_BEFORE=%%C"
"%SQLITE_CMD%" "%SQLITE_DIR%\wurmlogin.db" "DELETE FROM HISTORY; UPDATE SERVERS SET NAME='%WORLD_NAME%', MAPNAME='%WORLD_NAME%', SPAWNPOINTJENNX=0, SPAWNPOINTJENNY=0, SPAWNPOINTMOLX=0, SPAWNPOINTMOLY=0, SPAWNPOINTLIBX=0, SPAWNPOINTLIBY=0, HOMESERVER=1, KINGDOM=4, PVP=0, LOCAL=1; DELETE FROM SERVERPROPERTIES WHERE PROPKEY IN ('HOMESERVER_KINGDOM','AUTO_NETWORKING'); INSERT INTO SERVERPROPERTIES (PROPKEY, PROPVAL) VALUES ('HOMESERVER_KINGDOM', '4'); INSERT INTO SERVERPROPERTIES (PROPKEY, PROPVAL) VALUES ('AUTO_NETWORKING', 'false');"
echo   OK  wurmlogin: wiped HISTORY (%HISTORY_BEFORE%), set NAME/MAPNAME=%WORLD_NAME%, zeroed spawn points, forced Freedom home (HOMESERVER=1, KINGDOM=4, PVP=0, LOCAL=1)
echo   OK  SERVERPROPERTIES: HOMESERVER_KINGDOM=4 (Freedom), AUTO_NETWORKING=false (yaml externalIp wins)

rem Signal to the PostgresBackend mod that the next boot should DROP its
rem per-world database before re-importing from this fresh SQLite scaffold.
rem Otherwise Postgres would retain the previous world's state.
type nul > "%SQLITE_DIR%\.wurmmodloader-fresh-world"
echo   OK  wrote .wurmmodloader-fresh-world marker (Postgres will DROP+reimport on next boot)

echo.
echo Clean rebuild complete.
echo Next: start the server with start=^<WorldName^>; WorldSeedBootstrap will pick
echo        a landmass-centroid tile and seed one starter village at first boot.

if "%PAUSE_AT_END%"=="1" pause

endlocal
exit /b 0
