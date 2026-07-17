#!/bin/bash
# ==============================================================================
# start_all_sprint2.sh (Linux/macOS)
# Script di avvio automatico end-to-end per lo Sprint 2 di CargoService
#
# Uso:
#   ./start_all_sprint2.sh
#       -> apre un terminale per ogni servizio
#
#   ./start_all_sprint2.sh ubuntu
#       -> apre una sola finestra GNOME Terminal con più schede
# ==============================================================================

# Rileva la cartella radice Progetto
PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)

# Modalità terminale
UBUNTU_MODE=false
if [ "$1" = "ubuntu" ]; then
    UBUNTU_MODE=true
fi

TERMINAL_STARTED=false

echo "=============================================================================="
echo "Avvio completo del sistema Sprint 2 nella cartella: $PROJECT_DIR"
echo "=============================================================================="

# ==============================================================================
# 1. PULIZIA PREVENTIVA
# ==============================================================================

echo "[1/3] Pulizia preventiva porte e container Docker (8090, 8086, 8020, 8050-8053)..."

# Ferma container Docker
if command -v docker >/dev/null 2>&1; then

    (
        cd "$PROJECT_DIR/sprint2/robotsmart26/yamls" &&
        docker compose -f unibobasic26.yaml down 2>/dev/null
    ) || true

    docker rm -f wenv robotoutgui25 mosquitto 2>/dev/null || true

fi


# Libera porte
for port in 8086 8090 8085 8020 8050 8051 8052 8053; do

    if command -v fuser >/dev/null 2>&1; then
        fuser -k -n tcp $port 2>/dev/null || true

    elif command -v lsof >/dev/null 2>&1; then
        lsof -ti:$port | xargs kill -9 2>/dev/null || true

    fi

done


# Ferma Gradle daemon
if [ -f "$PROJECT_DIR/sprint2/prototype/ioport/gradlew" ]; then
    "$PROJECT_DIR/sprint2/prototype/ioport/gradlew" --stop 2>/dev/null || true
fi


echo "Attesa pulizia completata..."
sleep 2


# Crea rete Docker
if command -v docker >/dev/null 2>&1; then
    docker network create iss-network 2>/dev/null || true
fi


# ==============================================================================
# Funzione apertura terminali
# ==============================================================================

open_terminal() {

    local title="$1"
    local dir="$2"
    local cmd="$3"

    echo " -> Avvio [$title]..."


    # Modalità Ubuntu: una finestra con tab
    if $UBUNTU_MODE && command -v gnome-terminal >/dev/null 2>&1; then


        if [ "$TERMINAL_STARTED" = false ]; then

            TERMINAL_STARTED=true

            gnome-terminal \
                --title="$title" \
                --working-directory="$dir" \
                -- bash -c "cd \"$dir\" && $cmd; exec bash" &


        else

            gnome-terminal \
                --tab \
                --title="$title" \
                --working-directory="$dir" \
                -- bash -c "cd \"$dir\" && $cmd; exec bash" &

        fi


    # Modalità normale: comportamento originale
    elif command -v gnome-terminal >/dev/null 2>&1; then

        gnome-terminal \
            --title="$title" \
            --working-directory="$dir" \
            -- bash -c "trap 'exit 0' TERM INT HUP; : QAK_TERMINAL_$title; cd \"$dir\" && $cmd"


    elif command -v kitty >/dev/null 2>&1; then

        kitty \
            --title "$title" \
            --directory "$dir" \
            bash -c "trap 'exit 0' TERM INT HUP; : QAK_TERMINAL_$title; cd \"$dir\" && $cmd" &


    elif command -v x-terminal-emulator >/dev/null 2>&1; then

        x-terminal-emulator \
            -T "$title" \
            -e bash -c "trap 'exit 0' TERM INT HUP; : QAK_TERMINAL_$title; cd \"$dir\" && $cmd" &


    else

        xterm \
            -T "$title" \
            -e bash -c "trap 'exit 0' TERM INT HUP; : QAK_TERMINAL_$title; cd \"$dir\" && $cmd" &

    fi
}


echo "[2/3] Avvio sequenziale dei 7 terminali nell'ordine corretto..."


# ==============================================================================
# SERVIZI
# ==============================================================================


# 1 - WEnv Docker
open_terminal \
"1_WEnv_Docker" \
"$PROJECT_DIR/sprint2/robotsmart26/yamls" \
"docker compose -f unibobasic26.yaml up"

echo "Attesa avvio WEnv..."
sleep 6



# 2 - RobotSmart26
open_terminal \
"2_RobotSmart26" \
"$PROJECT_DIR/sprint2/robotsmart26" \
"./gradlew run --no-daemon"

sleep 5



# 3 - CargoService
open_terminal \
"3_CargoService" \
"$PROJECT_DIR/sprint2/prototype/cargoservice" \
"./gradlew run --no-daemon"

sleep 4



# 4 - CargoRobot
open_terminal \
"4_CargoRobot" \
"$PROJECT_DIR/sprint2/prototype/cargorobot" \
"./gradlew run --no-daemon"

sleep 3



# 5 - Ioport
open_terminal \
"5_Ioport_LedMock" \
"$PROJECT_DIR/sprint2/prototype/ioport" \
"./gradlew runIoport --no-daemon"

sleep 3



# 6 - Ioport Backend
open_terminal \
"6_Ioport_Backend" \
"$PROJECT_DIR/sprint2/prototype/ioport-backend" \
"./gradlew run --no-daemon"

sleep 3



# 7 - Devices
open_terminal \
"7_Devices_SonarMarker" \
"$PROJECT_DIR/sprint2/prototype/devices" \
"./gradlew run --no-daemon"

sleep 2



# ==============================================================================
# Browser
# ==============================================================================

echo "[3/3] Apertura Firefox su WEnv e Web GUI..."

if command -v firefox >/dev/null 2>&1; then

    firefox \
        "http://localhost:8090" \
        "http://localhost:8086" &

elif command -v xdg-open >/dev/null 2>&1; then

    xdg-open "http://localhost:8090" &
    xdg-open "http://localhost:8086" &

fi



echo "=============================================================================="
echo "Tutti i servizi Sprint 2 sono stati avviati!"
echo "Web GUI: http://localhost:8086"
echo "=============================================================================="