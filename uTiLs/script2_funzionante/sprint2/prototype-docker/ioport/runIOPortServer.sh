#!/bin/bash
echo "========================================================================="
echo "Avvio IOPortServer (Web GUI su porta 8086 e CoAP Observer)..."
echo "========================================================================="

# Controlla e libera la porta 8086 se occupata da processi precedenti
if command -v fuser >/dev/null 2>&1; then
    fuser -k -n tcp 8086 2>/dev/null || true
elif command -v lsof >/dev/null 2>&1; then
    lsof -ti:8086 | xargs kill -9 2>/dev/null || true
fi

# Esegui il server tramite Gradle o Java diretto
if [ -f "./gradlew" ]; then
    ./gradlew runIOPortServer --no-daemon
else
    gradle runIOPortServer --no-daemon
fi
