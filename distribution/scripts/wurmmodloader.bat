@echo off
REM WurmModLoader launcher script

cd /d "%~dp0"

setlocal enabledelayedexpansion

call :resolveJar "wurmmodloader-api-*.jar" API_JAR || goto :eof
call :resolveJar "wurmmodloader-core-*.jar" CORE_JAR || goto :eof
call :resolveJar "wurmmodloader-modsupport-*.jar" MODSUPPORT_JAR || goto :eof
call :resolveJar "wurmmodloader-legacy-*.jar" LEGACY_JAR || goto :eof

REM Build classpath
set CP=%API_JAR%
set CP=%CP%;%CORE_JAR%
set CP=%CP%;%MODSUPPORT_JAR%
set CP=%CP%;%LEGACY_JAR%
set CP=%CP%;javassist.jar
set CP=%CP%;gson.jar
set CP=%CP%;snakeyaml-2.2.jar
set CP=%CP%;server.jar
set CP=%CP%;common.jar

REM Launch modloader
java -cp "%CP%" -Djava.util.logging.config.file=logging.properties ^
    com.garward.wurmmodloader.serverlauncher.ServerLauncher %*

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
