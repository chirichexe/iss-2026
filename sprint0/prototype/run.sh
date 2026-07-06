#!/bin/bash
# Script di avvio rapido per i contesti QAK dello Sprint 0
# Posizionato in /sprint0/prototype per una maggiore comodità!

# Naviga automaticamente nel progetto cargosystem prima di eseguire gradlew
cd "$(dirname "$0")/cargosystem" || { echo "Cartella cargosystem non trovata!"; exit 1; }

case "$1" in
    c|cargoservice)
        echo -e "\033[1;34m=== Avvio Contesto: ctxcargoservice (porta 8050) ===\033[0m"
        ./gradlew runCargoservice
        ;;
    iop|ioport|i)
        echo -e "\033[1;36m=== Avvio Contesto: ctxioport (porta 8051) ===\033[0m"
        ./gradlew runIoport
        ;;
    robot|r)
        echo -e "\033[1;35m=== Avvio Contesto: ctxrobot (porta 8052) ===\033[0m"
        ./gradlew runRobot
        ;;
    devices|d|sonar|hold)
        echo -e "\033[1;33m=== Avvio Contesto: ctxdevices (porta 8053) ===\033[0m"
        ./gradlew runDevices
        ;;
    test|t|testplan)
        echo -e "\033[1;32m=== Esecuzione JUnit TestPlan ===\033[0m"
        ./gradlew test --tests "test.TestPlanSprint0" -i
        ;;
    *)
        echo -e "\033[1;31mUso errato o comando mancante.\033[0m"
        echo "Utilizza lo script in questo modo dalla cartella prototype:"
        echo ""
        echo "  ./run.sh cargoservice   (oppure  ./run.sh c )    -> Avvia ctxcargoservice"
        echo "  ./run.sh ioport         (oppure  ./run.sh iop )  -> Avvia ctxioport"
        echo "  ./run.sh robot          (oppure  ./run.sh r )    -> Avvia ctxrobot"
        echo "  ./run.sh devices        (oppure  ./run.sh d )    -> Avvia ctxdevices"
        echo "  ./run.sh test           (oppure  ./run.sh t )    -> Esegue il TestPlan JUnit"
        echo ""
        exit 1
        ;;
esac
