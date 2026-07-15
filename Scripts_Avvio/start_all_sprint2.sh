#!/bin/bash
# ==============================================================================
# start_all_sprint2.sh (Linux/macOS)
# Script di avvio automatico end-to-end per lo Sprint 2 di CargoService
# ==============================================================================

# Rileva la cartella radice Progetto (essendo questo script in Scripts_Avvio)
PROJECT_DIR=$(cd "$(dirname "$0")/.." && pwd)
echo "=============================================================================="
echo "Avvio completo del sistema Sprint 2 nella cartella: $PROJECT_DIR"
echo "=============================================================================="

# 1. PULIZIA PREVENTIVA DI TUTTE LE PORTE E PROCESSI IN ESECUZIONE
echo "[1/3] Pulizia preventiva porte e container Docker (8090, 8086, 8020, 8050-8053)..."

# Ferma eventuali container Docker in esecuzione dal yaml di robotsmart
if command -v docker >/dev/null 2>&1; then
    (cd "$PROJECT_DIR/sprint2/robotsmart26/yamls" && docker compose -f unibobasic26.yaml down 2>/dev/null) || true
    docker rm -f wenv robotoutgui25 mosquitto 2>/dev/null || true
fi

# Libera porte QAK, CoAP, Web GUI e WEnv
for port in 8086 8090 8085 8020 8050 8051 8052 8053; do
    if command -v fuser >/dev/null 2>&1; then
        fuser -k -n tcp $port 2>/dev/null || true
    elif command -v lsof >/dev/null 2>&1; then
        lsof -ti:$port | xargs kill -9 2>/dev/null || true
    fi
done

# Termina tutti i Gradle daemon inattivi in RAM per evitare Out of Memory (OOM Killer)
if [ -f "$PROJECT_DIR/sprint2/prototype/customer/gradlew" ]; then
    "$PROJECT_DIR/sprint2/prototype/customer/gradlew" --stop 2>/dev/null || true
fi

echo "Attesa pulizia completata..."
sleep 2

# Assicurati che esista la rete Docker richiesta da unibobasic26.yaml
if command -v docker >/dev/null 2>&1; then
    docker network create iss-network 2>/dev/null || true
fi

# Funzione per aprire una nuova finestra del terminale
open_terminal() {
    local title="$1"
    local dir="$2"
    local cmd="$3"
    echo " -> Avvio [$title]..."
    if command -v gnome-terminal >/dev/null 2>&1; then
        gnome-terminal --title="$title" --working-directory="$dir" -- bash -c "trap 'exit 0' TERM INT HUP; : QAK_TERMINAL_$title; cd \"$dir\" && $cmd"
    elif command -v kitty >/dev/null 2>&1; then
        kitty --title "$title" --directory "$dir" bash -c "trap 'exit 0' TERM INT HUP; : QAK_TERMINAL_$title; cd \"$dir\" && $cmd" &
    elif command -v x-terminal-emulator >/dev/null 2>&1; then
        x-terminal-emulator -T "$title" -e bash -c "trap 'exit 0' TERM INT HUP; : QAK_TERMINAL_$title; cd \"$dir\" && $cmd" &
    else
        xterm -T "$title" -e bash -c "trap 'exit 0' TERM INT HUP; : QAK_TERMINAL_$title; cd \"$dir\" && $cmd" &
    fi
}

echo "[2/3] Avvio sequenziale dei 7 terminali nell'ordine corretto di dipendenza..."

# TERMINALE 1: Ambiente Virtuale WEnv e Broker MQTT (docker-compose)
open_terminal "1_WEnv_Docker" "$PROJECT_DIR/sprint2/robotsmart26/yamls" "docker compose -f unibobasic26.yaml up"
echo "    Attesa avvio WEnv (6 secondi)..."
sleep 6

# TERMINALE 2: Servizio Base Robot e Pathfinder (robotsmart26)
open_terminal "2_RobotSmart26" "$PROJECT_DIR/sprint2/robotsmart26" "./gradlew run --no-daemon"
echo "    Attesa avvio RobotSmart26 (5 secondi)..."
sleep 5

# TERMINALE 3: Orchestratore Centrale e Risorsa CoAP (cargoservice)
open_terminal "3_CargoService" "$PROJECT_DIR/sprint2/prototype/cargoservice" "./gradlew run --no-daemon"
echo "    Attesa avvio CargoService (4 secondi)..."
sleep 4

# TERMINALE 4: Wrapper Trasportatore del Robot (cargorobot)
open_terminal "4_CargoRobot" "$PROJECT_DIR/sprint2/prototype/robot" "./gradlew run --no-daemon"
echo "    Attesa avvio CargoRobot (3 secondi)..."
sleep 3

# TERMINALE 5: Contesto QAK Cliente / LedMock (customer)
open_terminal "5_Customer_LedMock" "$PROJECT_DIR/sprint2/prototype/customer" "./gradlew runCustomer --no-daemon"
echo "    Attesa avvio Customer context (3 secondi)..."
sleep 3

# TERMINALE 6: Server Esterno Web GUI Inbound Adapter su porta 8086 (guiserver26qak0)
open_terminal "6_GuiServer_Web" "$PROJECT_DIR/sprint2/prototype/guiserver" "./gradlew run --no-daemon"
echo "    Attesa avvio GuiServer (3 secondi)..."
sleep 3

# TERMINALE 7: Dispositivi Sonar e Marker per il test fisico (devices)
open_terminal "7_Devices_SonarMarker" "$PROJECT_DIR/sprint2/prototype/devices" "./gradlew run --no-daemon"
echo "    Attesa avvio Devices (2 secondi)..."
sleep 2

# 3. APERTURA DI FIREFOX SU WENV (8090) E WEB GUI (8086)
echo "[3/3] Apertura di Firefox su WEnv (porta 8090) e Web GUI (porta 8086)..."
if command -v firefox >/dev/null 2>&1; then
    firefox "http://localhost:8090" "http://localhost:8086" &
elif command -v xdg-open >/dev/null 2>&1; then
    xdg-open "http://localhost:8090" &
    xdg-open "http://localhost:8086" &
fi

echo "=============================================================================="
echo "Tutti i 7 terminali e il browser sono stati avviati!"
echo "Puoi interagire direttamente dalla pagina Web all'indirizzo http://localhost:8086"
echo "=============================================================================="
