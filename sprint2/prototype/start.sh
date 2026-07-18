echo "=============================================================================="
echo "Build applicazioni Gradle..."
echo "=============================================================================="

(
    cd ioport-backend
    ./gradlew distTar
)

(
    cd cargoservice
    ./gradlew distTar
)

(
    cd cargorobot
    ./gradlew distTar
)

(
    cd devices
    ./gradlew distTar
)

echo "Build Gradle completata"

echo "=============================================================================="
echo "Avvio sistema Sprint 2 (Containerizzato) in corso..."
echo "=============================================================================="

# Assicuriamoci che la rete esista
docker network create iss-network 2>/dev/null || true

# Eseguiamo la build e l'avvio in background
docker compose up --build -d

echo ""
echo "=============================================================================="
echo "Tutti i container sono stati avviati con successo!"
echo "Puoi monitorare i log con: docker compose logs -f"
echo ""
echo "APRI IL BROWSER AI SEGUENTI INDIRIZZI:"
echo "👉 http://localhost:8086/  (per la Web GUI / IOPORT)"
echo "👉 http://localhost:8090/  (per la Scena Virtuale del ROBOT / WEnv)"
echo "=============================================================================="
