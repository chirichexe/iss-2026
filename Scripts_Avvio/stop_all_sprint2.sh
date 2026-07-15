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

echo "Tutti i servizi dello Sprint 2 sono stati arrestati con successo!"
