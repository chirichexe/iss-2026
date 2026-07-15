@echo off
REM ==============================================================================
REM stop_all_sprint2.bat (Windows)
REM Script per arrestare e pulire tutti i processi dello Sprint 2
REM ==============================================================================

set PROJECT_DIR=%~dp0..
echo Arresto di tutti i servizi QAK, Docker e Web GUI in corso...

cd /d "%PROJECT_DIR%\sprint2\robotsmart26\yamls"
docker compose -f unibobasic26.yaml down 2>nul
docker rm -f wenv robotoutgui25 mosquitto 2>nul

for %%p in (8086 8090 8085 8020 8050 8051 8052 8053) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr /R /C:":%%p .*LISTENING"') do (
        taskkill /F /PID %%a 2>nul
    )
)

echo Tutti i servizi dello Sprint 2 sono stati arrestati con successo su Windows!
