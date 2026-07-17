#!/bin/bash
# ==============================================================================
# stop_all_sprint2.sh (Linux/macOS)
# Script per arrestare e pulire tutti i processi dello Sprint 2
# ==============================================================================

PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)
echo "Arresto di tutti i servizi QAK, Docker e Web GUI in corso..."

if command -v docker >/dev/null 2>&1; then
    (cd "$PROJECT_DIR/sprint2/robotsmart26/yamls" && docker compose -f unibobasic26.yaml down 2>/dev/null) || true
    docker rm -f wenv robotoutgui25 mosquitto 2>/dev/null || true
fi

for port in 8086 8090 8085 8020 8050 8051 8052 8053; do
    if command -v fuser >/dev/null 2>&1; then
        fuser -k -n tcp $port 2>/dev/null || true
    elif command -v lsof >/dev/null 2>&1; then
        lsof -ti:$port | xargs kill -9 2>/dev/null || true
    fi
done

# Chiusura delle finestre dei terminali aperte dagli script di avvio
echo "Chiusura delle finestre dei terminali in corso..."
pkill -9 -f "QAK_TERMINAL_" 2>/dev/null || true
for title in "1_WEnv_Docker" "2_RobotSmart26" "3_CargoService" "4_CargoRobot" "5_Ioport_LedMock" "6_Ioport_Backend" "7_Devices_SonarMarker"; do
    pkill -f "$title" 2>/dev/null || true
    if command -v wmctrl >/dev/null 2>&1; then
        wmctrl -F -c "$title" 2>/dev/null || true
    fi
    if command -v xdotool >/dev/null 2>&1; then
        xdotool search --name "$title" windowclose 2>/dev/null || true
    fi
done
pkill -f "runIoport" 2>/dev/null || true
pkill -f "runDevices" 2>/dev/null || true
pkill -f "runRobot" 2>/dev/null || true
pkill -f "runIOPortServer.sh" 2>/dev/null || true
pkill -f "GuiServerMain" 2>/dev/null || true

echo "Tutti i servizi e le finestre dei terminali dello Sprint 2 sono stati arrestati e chiusi con successo!"
