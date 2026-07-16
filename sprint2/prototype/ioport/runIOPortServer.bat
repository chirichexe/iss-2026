@echo off
echo =========================================================================
echo Avvio IOPortServer (Web GUI su porta 8086 e CoAP Observer)...
echo =========================================================================

REM Libera la porta 8086 su Windows se occupata da processi precedenti
for /f "tokens=5" %%a in ('netstat -aon ^| findstr /R /C:":8086 .*LISTENING"') do (
    taskkill /F /PID %%a 2>nul
)

if exist gradlew.bat (
    call gradlew.bat runIOPortServer --no-daemon
) else (
    gradle runIOPortServer --no-daemon
)
