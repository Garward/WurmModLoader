@echo off
REM WurmModLoader patcher script

cd /d "%~dp0"

REM Patcher JAR is self-contained (shadow JAR with Javassist)
java -jar wurmmodloader-patcher-1.0.0-SNAPSHOT.jar %*
