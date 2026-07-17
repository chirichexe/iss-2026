#!/bin/bash
# ==============================================================================
# run_tests.sh
# Integration tests for Sprint 2 (Maritime Cargo shipping company)
# ==============================================================================

set -e

# Setup directories
SCRIPT_DIR=$(cd "$(dirname "$0")" && pwd)
PROJECT_DIR=$(cd "$SCRIPT_DIR/.." && pwd)
LOGS_DIR="$SCRIPT_DIR/test_logs"

mkdir -p "$LOGS_DIR"

echo "=============================================================================="
echo "Starting Integration Tests Suite for Sprint 2"
echo "=============================================================================="

# 1. CLEANUP PREVENTIVELY
echo "Cleaning up existing processes and Docker containers..."
if command -v docker >/dev/null 2>&1; then
    (cd "$PROJECT_DIR/sprint2/robotsmart26/yamls" && docker compose -f unibobasic26.yaml down 2>/dev/null) || true
    docker rm -f wenv robotoutgui25 mosquitto 2>/dev/null || true
fi

# Kill any processes running on the system ports
for port in 8086 8090 8085 8020 8050 8051 8052 8053; do
    if command -v fuser >/dev/null 2>&1; then
        fuser -k -n tcp $port 2>/dev/null || true
    elif command -v lsof >/dev/null 2>&1; then
        lsof -ti:$port | xargs kill -9 2>/dev/null || true
    fi
done

# Terminate gradle daemons to save memory
"$PROJECT_DIR/sprint2/prototype/ioport/gradlew" --stop 2>/dev/null || true

# Wait for cleanup to settle
sleep 2

# Create Docker network if needed
if command -v docker >/dev/null 2>&1; then
    docker network create iss-network 2>/dev/null || true
fi

# 2. START INFRASTRUCTURE IN BACKGROUND
echo "Starting Docker Compose services (WEnv, mosquitto, robotoutgui)..."
(cd "$PROJECT_DIR/sprint2/robotsmart26/yamls" && docker compose -f unibobasic26.yaml up -d)

# Wait for Docker containers to start
sleep 6

declare -A pids

# Start headless Firefox to serve as the WebGL master scene for WEnv
echo "Starting headless Firefox as WEnv WebGL master..."
firefox --headless -no-remote -CreateProfile "agy_test" 2>/dev/null || true
firefox --headless -no-remote -P "agy_test" http://localhost:8090/ > /dev/null 2>&1 &
pids[firefox]=$!
sleep 10

echo "Starting RobotSmart26 base..."
cd "$PROJECT_DIR/sprint2/robotsmart26"
./gradlew run --no-daemon > "$LOGS_DIR/robotsmart26.log" 2>&1 &
pids[robotsmart26]=$!

echo "Starting CargoService..."
cd "$PROJECT_DIR/sprint2/prototype/cargoservice"
./gradlew run --no-daemon > "$LOGS_DIR/cargoservice.log" 2>&1 &
pids[cargoservice]=$!

echo "Starting CargoRobot wrapper..."
cd "$PROJECT_DIR/sprint2/prototype/robot"
./gradlew run --no-daemon > "$LOGS_DIR/cargorobot.log" 2>&1 &
pids[cargorobot]=$!

echo "Starting Ioport context..."
cd "$PROJECT_DIR/sprint2/prototype/ioport"
./gradlew runIoport --no-daemon > "$LOGS_DIR/ioport.log" 2>&1 &
pids[ioport]=$!

echo "Starting GuiServer web interface..."
cd "$PROJECT_DIR/sprint2/prototype/guiserver"
./gradlew run --no-daemon > "$LOGS_DIR/guiserver.log" 2>&1 &
pids[guiserver]=$!

echo "Starting Devices context (configured to SONAR_TOPIC=sensore/sonar/test)..."
cd "$PROJECT_DIR/sprint2/prototype/devices"
export SONAR_TOPIC=sensore/sonar/test
./gradlew run --no-daemon > "$LOGS_DIR/devices.log" 2>&1 &
pids[devices]=$!

# Function to clean up background processes on exit
cleanup() {
    echo "=============================================================================="
    echo "Shutting down all test processes and cleaning up..."
    echo "=============================================================================="
    for name in "${!pids[@]}"; do
        pid=${pids[$name]}
        if kill -0 $pid 2>/dev/null; then
            echo "Killing background process $name (PID $pid)..."
            kill -9 $pid || true
        fi
    done
    if command -v docker >/dev/null 2>&1; then
        (cd "$PROJECT_DIR/sprint2/robotsmart26/yamls" && docker compose -f unibobasic26.yaml down 2>/dev/null) || true
    fi
}
trap cleanup EXIT

# 3. VERIFY CONNECTION AND WARM UP
echo "Waiting for all QAK contexts and web servers to start up and connect..."

# Wait for cargoservice (port 8050)
while ! (echo > /dev/tcp/127.0.0.1/8050) >/dev/null 2>&1; do
    echo "Waiting for cargoservice on port 8050..."
    sleep 2
done
echo "cargoservice is online."

# Wait for ioport (port 8051)
while ! (echo > /dev/tcp/127.0.0.1/8051) >/dev/null 2>&1; do
    echo "Waiting for ioport context on port 8051..."
    sleep 2
done
echo "ioport context is online."

# Wait for devices (port 8052)
while ! (echo > /dev/tcp/127.0.0.1/8052) >/dev/null 2>&1; do
    echo "Waiting for devices context on port 8052..."
    sleep 2
done
echo "devices context is online."

# Wait for robot (port 8053)
while ! (echo > /dev/tcp/127.0.0.1/8053) >/dev/null 2>&1; do
    echo "Waiting for robot context on port 8053..."
    sleep 2
done
echo "robot context is online."

# Wait for guiserver (port 8086)
while ! (echo > /dev/tcp/127.0.0.1/8086) >/dev/null 2>&1; do
    echo "Waiting for guiserver on port 8086..."
    sleep 2
done
echo "guiserver is online."

# Wait an additional 4 seconds for CoAP observer relation to be established
sleep 4

# Publish initial FREE distance (e.g. 15 cm) to establish working state and free IOPort
echo "Publishing initial sonar distance 15 cm to MQTT sensore/sonar/test..."
docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "15.0"
sleep 2

# Verify initial state
echo "Checking initial cargosystem state..."
STATE_JSON=$(curl -s http://localhost:8086/api/state)
echo "Initial State: $STATE_JSON"

# Check that serviceState is disengaged, workingState is Service working, and ioPortOccupied is false
SERVICE_STATE=$(echo "$STATE_JSON" | jq -r '.serviceState')
WORKING_STATE=$(echo "$STATE_JSON" | jq -r '.workingState')
IOPORT_OCCUPIED=$(echo "$STATE_JSON" | jq -r '.ioPortOccupied')

if [ "$SERVICE_STATE" != "disengaged" ] || [ "$WORKING_STATE" != "Service working" ] || [ "$IOPORT_OCCUPIED" != "false" ]; then
    echo "ERROR: Initial state does not match requirements!"
    exit 1
fi
echo "Success: Initial state requirements met."

# 4. RUN TEST SCENARIOS

# ==============================================================================
# TEST 1: Accepted request and Deposit Timeout
# ==============================================================================
echo "------------------------------------------------------------------------------"
echo "TEST 1: Request Load -> Accepted -> Deposit Timeout after 30s"
echo "------------------------------------------------------------------------------"

# Request load via HTTP POST
REQ_RESPONSE=$(curl -s -X POST http://localhost:8086/api/load)
echo "POST /api/load response: $REQ_RESPONSE"

STATUS=$(echo "$REQ_RESPONSE" | jq -r '.status')
SLOT=$(echo "$REQ_RESPONSE" | jq -r '.slot')

if [ "$STATUS" != "accepted" ] || [ "$SLOT" != "slot1" ]; then
    echo "ERROR: Load request should have been accepted with slot1, got status=$STATUS slot=$SLOT"
    exit 1
fi
echo "Success: Load request accepted and assigned to $SLOT."

# Verify system is now engaged
STATE_JSON=$(curl -s http://localhost:8086/api/state)
echo "Current State: $STATE_JSON"
SERVICE_STATE=$(echo "$STATE_JSON" | jq -r '.serviceState')
if [ "$SERVICE_STATE" != "engaged" ]; then
    echo "ERROR: System should be engaged after accepted request, but was $SERVICE_STATE"
    exit 1
fi
echo "Success: System state is engaged."

# Verify load request while engaged returns retrylater
REQ_ENGAGED_RESPONSE=$(curl -s -X POST http://localhost:8086/api/load)
echo "POST /api/load response (while engaged): $REQ_ENGAGED_RESPONSE"
STATUS_ENGAGED=$(echo "$REQ_ENGAGED_RESPONSE" | jq -r '.status')
if [ "$STATUS_ENGAGED" != "retrylater" ]; then
    echo "ERROR: Should have returned retrylater when engaged, got $STATUS_ENGAGED"
    exit 1
fi
echo "Success: Load request refused with retrylater during engaged state."

# Wait 32 seconds to trigger deposit timeout (the requirement is 30s)
echo "Waiting 32 seconds for deposit timeout to expire..."
sleep 32

# Verify system is now disengaged again
STATE_JSON=$(curl -s http://localhost:8086/api/state)
echo "State after timeout: $STATE_JSON"
SERVICE_STATE=$(echo "$STATE_JSON" | jq -r '.serviceState')
if [ "$SERVICE_STATE" != "disengaged" ]; then
    echo "ERROR: System should have disengaged after timeout, but was $SERVICE_STATE"
    exit 1
fi
echo "Success: System correctly disengaged after timeout."

# ==============================================================================
# TEST 2: Successful Load, Marking, and Storage in Slot 1
# ==============================================================================
echo "------------------------------------------------------------------------------"
echo "TEST 2: Load Request -> Accepted -> Container Deposit -> Robot marking and deposit"
echo "------------------------------------------------------------------------------"

# Request load again
REQ_RESPONSE=$(curl -s -X POST http://localhost:8086/api/load)
echo "POST /api/load response: $REQ_RESPONSE"

STATUS=$(echo "$REQ_RESPONSE" | jq -r '.status')
SLOT=$(echo "$REQ_RESPONSE" | jq -r '.slot')

if [ "$STATUS" != "accepted" ] || [ "$SLOT" != "slot1" ]; then
    echo "ERROR: Load request should have been accepted with slot1"
    exit 1
fi

# Simulate container deposit: publish D < 10 (e.g. 5.0 cm) sustained for 3 seconds
echo "Simulating container deposit (D = 5.0 cm)..."
docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "5.0"
sleep 1.5
docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "5.0"
sleep 2.0
docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "5.0"
sleep 1

# Verify IOPort is occupied
STATE_JSON=$(curl -s http://localhost:8086/api/state)
echo "State during deposit: $STATE_JSON"
IOPORT_OCCUPIED=$(echo "$STATE_JSON" | jq -r '.ioPortOccupied')
if [ "$IOPORT_OCCUPIED" != "true" ]; then
    echo "ERROR: IOPort should be detected as occupied (container present)"
    exit 1
fi
echo "Success: IOPortOccupied is true."

# Verify load request while occupied returns retrylater
REQ_OCCUPIED_RESPONSE=$(curl -s -X POST http://localhost:8086/api/load)
echo "POST /api/load response (while occupied): $REQ_OCCUPIED_RESPONSE"
STATUS_OCCUPIED=$(echo "$REQ_OCCUPIED_RESPONSE" | jq -r '.status')
if [ "$STATUS_OCCUPIED" != "retrylater" ]; then
    echo "ERROR: Should have returned retrylater when occupied, got $STATUS_OCCUPIED"
    exit 1
fi
echo "Success: Load request refused with retrylater during occupied state."

# The robot will now fetch the container, mark it in slot 5, and move it to slot 1.
# This operation takes some time because of the pathfinder movements. Let's wait.
echo "Waiting for cargorobot to mark and store the container in slot1 (approx 20 seconds)..."
sleep 28

# Verify that slot1 is now occupied, and IOPortOccupied is false again
STATE_JSON=$(curl -s http://localhost:8086/api/state)
echo "Current State: $STATE_JSON"

SLOT1_STATE=$(echo "$STATE_JSON" | jq -r '.slots.slot1')
IOPORT_OCCUPIED=$(echo "$STATE_JSON" | jq -r '.ioPortOccupied')
SERVICE_STATE=$(echo "$STATE_JSON" | jq -r '.serviceState')

if [ "$SLOT1_STATE" != "occupied" ] || [ "$IOPORT_OCCUPIED" != "false" ] || [ "$SERVICE_STATE" != "disengaged" ]; then
    echo "ERROR: Slot1 should be occupied, IOPortOccupied false, and service disengaged. Got slot1=$SLOT1_STATE ioPortOccupied=$IOPORT_OCCUPIED serviceState=$SERVICE_STATE"
    exit 1
fi
echo "Success: Container successfully moved from IOPort to slot1!"

# ==============================================================================
# TEST 3: Out of Service Detection and Recovery
# ==============================================================================
echo "------------------------------------------------------------------------------"
echo "TEST 3: Sonar D > DFREE -> Out of Service -> Recovery"
echo "------------------------------------------------------------------------------"

# Publish D > 20 (e.g. 25.0 cm) to trigger Out of service failure (sustained for 3 seconds)
echo "Publishing sonar distance 25.0 cm (Out of Service threshold)..."
docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "25.0"
sleep 1.5
docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "25.0"
sleep 2.0
docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "25.0"
sleep 1

# Verify workingState is Out of service
STATE_JSON=$(curl -s http://localhost:8086/api/state)
echo "State after out-of-service publication: $STATE_JSON"
WORKING_STATE=$(echo "$STATE_JSON" | jq -r '.workingState')
if [ "$WORKING_STATE" != "Out of service" ]; then
    echo "ERROR: System should be Out of service"
    exit 1
fi
echo "Success: workingState is 'Out of service'."

# Attempt a load request while Out of service -> should reply retrylater
REQ_RESPONSE=$(curl -s -X POST http://localhost:8086/api/load)
echo "POST /api/load response (while Out of service): $REQ_RESPONSE"
STATUS=$(echo "$REQ_RESPONSE" | jq -r '.status')
if [ "$STATUS" != "retrylater" ]; then
    echo "ERROR: Should have returned retrylater when Out of service"
    exit 1
fi
echo "Success: Request refused with retrylater during Out of service."

# Recover: publish working distance again (e.g. 15.0 cm)
echo "Publishing sonar distance 15.0 cm to recover..."
docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "15.0"
sleep 2

# Verify workingState is Service working again
STATE_JSON=$(curl -s http://localhost:8086/api/state)
WORKING_STATE=$(echo "$STATE_JSON" | jq -r '.workingState')
if [ "$WORKING_STATE" != "Service working" ]; then
    echo "ERROR: System should have recovered to Service working"
    exit 1
fi
echo "Success: recovered to 'Service working'."

# ==============================================================================
# TEST 4: Full Hold Refusal
# ==============================================================================
echo "------------------------------------------------------------------------------"
echo "TEST 4: Load Slot 2, 3, 4 -> Attempt Slot 5 -> Refused"
echo "------------------------------------------------------------------------------"

# Let's occupy slot2, slot3, and slot4
for slot_id in 2 3 4; do
    echo "Loading container into slot $slot_id..."
    
    # Request load
    REQ_RESPONSE=$(curl -s -X POST http://localhost:8086/api/load)
    STATUS=$(echo "$REQ_RESPONSE" | jq -r '.status')
    SLOT=$(echo "$REQ_RESPONSE" | jq -r '.slot')
    echo "Assigned: status=$STATUS slot=$SLOT"
    
    # Publish D < 10
    docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "5.0"
    sleep 1.5
    docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "5.0"
    sleep 2.0
    docker exec mosquitto mosquitto_pub -t sensore/sonar/test -m "5.0"
    sleep 1
    
    # Wait for completion
    echo "Waiting for robot to store container in slot $slot_id..."
    sleep 28
done

# Verify all slots 1-4 are occupied
STATE_JSON=$(curl -s http://localhost:8086/api/state)
echo "Hold state before full check: $STATE_JSON"

SLOT1_STATE=$(echo "$STATE_JSON" | jq -r '.slots.slot1')
SLOT2_STATE=$(echo "$STATE_JSON" | jq -r '.slots.slot2')
SLOT3_STATE=$(echo "$STATE_JSON" | jq -r '.slots.slot3')
SLOT4_STATE=$(echo "$STATE_JSON" | jq -r '.slots.slot4')

if [ "$SLOT1_STATE" != "occupied" ] || [ "$SLOT2_STATE" != "occupied" ] || [ "$SLOT3_STATE" != "occupied" ] || [ "$SLOT4_STATE" != "occupied" ]; then
    echo "ERROR: Not all slots are occupied!"
    exit 1
fi
echo "Success: slots1-4 are all occupied."

# Attempt another load request -> should be refused
REQ_RESPONSE=$(curl -s -X POST http://localhost:8086/api/load)
echo "POST /api/load response (with full hold): $REQ_RESPONSE"
STATUS=$(echo "$REQ_RESPONSE" | jq -r '.status')
if [ "$STATUS" != "refused" ]; then
    echo "ERROR: Should have returned refused when hold is full, got $STATUS"
    exit 1
fi
echo "Success: Load request refused when hold is full."

echo "=============================================================================="
echo "ALL SPRINT 2 INTEGRATION TESTS PASSED SUCCESSFULLY!"
echo "=============================================================================="
