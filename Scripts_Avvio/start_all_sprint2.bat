@echo off
REM ==============================================================================
REM start_all_sprint2.bat (Windows)
REM Script di avvio automatico end-to-end per lo Sprint 2 di CargoService
REM ==============================================================================

set PROJECT_DIR=%~dp0..
echo ==============================================================================
echo Avvio completo del sistema Sprint 2 nella cartella: %PROJECT_DIR%
echo ==============================================================================

echo [1/3] Pulizia preventiva porte e container Docker (8090, 8086, 8020, 8050-8053)...

REM Ferma eventuali container Docker in esecuzione dal yaml di robotsmart
cd /d "%PROJECT_DIR%\sprint2\robotsmart26\yamls"
docker compose -f unibobasic26.yaml down 2>nul
docker rm -f wenv robotoutgui25 mosquitto 2>nul

REM Libera porte QAK, CoAP, Web GUI e WEnv su Windows
for %%p in (8086 8090 8085 8020 8050 8051 8052 8053) do (
    for /f "tokens=5" %%a in ('netstat -aon ^| findstr /R /C:":%%p .*LISTENING"') do (
        taskkill /F /PID %%a 2>nul
    )
)

if exist "%PROJECT_DIR%\sprint2\prototype\ioport\gradlew.bat" (
    call "%PROJECT_DIR%\sprint2\prototype\ioport\gradlew.bat" --stop 2>nul
)

timeout /t 2 /nobreak >nul
docker network create iss-network 2>nul

echo [2/3] Avvio sequenziale dei 7 terminali nell'ordine corretto di dipendenza...

REM TERMINALE 1: Ambiente Virtuale WEnv e Broker MQTT
echo  -> Avvio [1_WEnv_Docker]...
start "1_WEnv_Docker" /D "%PROJECT_DIR%\sprint2\robotsmart26\yamls" cmd /k "docker compose -f unibobasic26.yaml up"
echo     Attesa avvio WEnv (6 secondi)...
timeout /t 6 /nobreak >nul

REM TERMINALE 2: Servizio Base Robot e Pathfinder (robotsmart26)
echo  -> Avvio [2_RobotSmart26]...
start "2_RobotSmart26" /D "%PROJECT_DIR%\sprint2\robotsmart26" cmd /k "gradlew.bat run --no-daemon"
echo     Attesa avvio RobotSmart26 (5 secondi)...
timeout /t 5 /nobreak >nul

REM TERMINALE 3: Orchestratore Centrale e Risorsa CoAP (cargoservice)
echo  -> Avvio [3_CargoService]...
start "3_CargoService" /D "%PROJECT_DIR%\sprint2\prototype\cargoservice" cmd /k "gradlew.bat run --no-daemon"
echo     Attesa avvio CargoService (4 secondi)...
timeout /t 4 /nobreak >nul

REM TERMINALE 4: Wrapper Trasportatore del Robot (cargorobot)
echo  -> Avvio [4_CargoRobot]...
start "4_CargoRobot" /D "%PROJECT_DIR%\sprint2\prototype\cargorobot" cmd /k "gradlew.bat run --no-daemon"
echo     Attesa avvio CargoRobot (3 secondi)...
timeout /t 3 /nobreak >nul

REM TERMINALE 5: Contesto QAK Ioport / LedMock (ioport)
echo  -> Avvio [5_Ioport_LedMock]...
start "5_Ioport_LedMock" /D "%PROJECT_DIR%\sprint2\prototype\ioport" cmd /k "gradlew.bat runIoport --no-daemon"
echo     Attesa avvio Ioport context (3 secondi)...
timeout /t 3 /nobreak >nul

REM TERMINALE 6: Server Esterno Web GUI su porta 8086 (ioport-backend)
echo  -> Avvio [6_Ioport_Backend]...
start "6_Ioport_Backend" /D "%PROJECT_DIR%\sprint2\prototype\ioport-backend" cmd /k "gradlew.bat run --no-daemon"
echo     Attesa avvio ioport-backend (3 secondi)...
timeout /t 3 /nobreak >nul

REM TERMINALE 7: Dispositivi Sonar e Marker per il test fisico (devices)
echo  -> Avvio [7_Devices_SonarMarker]...
start "7_Devices_SonarMarker" /D "%PROJECT_DIR%\sprint2\prototype\devices" cmd /k "gradlew.bat run --no-daemon"
echo     Attesa avvio Devices (2 secondi)...
timeout /t 2 /nobreak >nul

echo [3/3] Apertura del browser all'indirizzo del WEnv (8090) e della Web GUI (8086)...
start http://localhost:8090
start http://localhost:8086

echo ==============================================================================
echo Tutti i 7 terminali e il browser sono stati avviati!
echo Puoi interagire direttamente dalla pagina Web all'indirizzo http://localhost:8086
echo ==============================================================================
