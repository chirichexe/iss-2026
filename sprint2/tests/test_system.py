import time
import json
import threading
import pytest
import requests
import paho.mqtt.client as mqtt
from websocket import create_connection

# Configurazioni degli Endpoint e dei Servizi
HTTP_BACKEND_URL = "http://localhost:8086/api/load"
WS_BACKEND_URL = "ws://localhost:8086/ws"
MQTT_BROKER = "localhost"
MQTT_PORT = 1883
MQTT_TOPIC_SONAR = "sonardata"

# Costanti di temporizzazione basate sui requisiti (in secondi)
TIME_STABLE_PRESENCE = 3
TIME_TIMEOUT_CLIENTE = 30
TIME_MARGIN = 1 # Margine per la latenza di rete e computazione QAK

class SystemTestProbe:
    """Classe Helper per gestire lo stato del SUT (System Under Test) e l'iniezione di eventi."""
    def __init__(self):
        self.ws = None
        self.mqtt_client = None
        self._current_distance = 15
        self._running = False
        self._thread = None

    def connect(self):
        self.mqtt_client = mqtt.Client(client_id="SystemTestProbe")
        self.mqtt_client.connect(MQTT_BROKER, MQTT_PORT, 60)
        self.mqtt_client.loop_start()
        self.ws = create_connection(WS_BACKEND_URL)
        
        self._running = True
        self._thread = threading.Thread(target=self._sonar_loop)
        self._thread.daemon = True
        self._thread.start()

    def _sonar_loop(self):
        """Invia continuamente la distanza al broker MQTT, formattata per QAK."""
        while self._running:
            # QAK MQTT event format: msg(MSGID, MSGTYPE, SENDER, RECEIVER, CONTENT, SEQNUM)
            msg = f"msg(wall_sonardata,event,systemtestprobe,none,distance({self._current_distance}),1)"
            self.mqtt_client.publish(MQTT_TOPIC_SONAR, msg)
            time.sleep(0.5)

    def disconnect(self):
        self._running = False
        if self._thread:
            self._thread.join(timeout=2)
        if self.ws:
            self.ws.close()
        if self.mqtt_client:
            self.mqtt_client.loop_stop()
            self.mqtt_client.disconnect()

    def send_sonar(self, distance):
        """Imposta la distanza corrente inviata dal loop del Sonar."""
        self._current_distance = distance

    def simulate_container_present(self):
        """Simula il posizionamento del container (distanza ridotta)."""
        self.send_sonar(5)

    def simulate_container_absent(self):
        """Simula l'assenza del container (distanza normale, < DFree ma > DFree/2)."""
        self.send_sonar(15)

    def simulate_out_of_service(self):
        """Simula un guasto al Sonar (in cargoservice.qak D > D_FREE (20) causa out of service)."""
        self.send_sonar(150)

    def send_load_request(self):
        """Invia una richiesta HTTP POST a /api/load e restituisce la response."""
        return requests.post(HTTP_BACKEND_URL)

    def wait_for_state(self, condition_func, timeout=10):
        """Legge dal WebSocket finché la condition_func non restituisce True, oppure va in timeout."""
        start_time = time.time()
        while time.time() - start_time < timeout:
            try:
                self.ws.settimeout(timeout - (time.time() - start_time))
                message = self.ws.recv()
                state = json.loads(message)
                if condition_func(state):
                    return state
            except Exception as e:
                pass
        pytest.fail(f"Timeout raggiunto aspettando una specifica condizione di stato.")
        return None

    def get_current_state(self):
        """Interroga lo stato attuale tramite HTTP GET."""
        resp = requests.get("http://localhost:8086/api/state")
        return resp.json() if resp.status_code == 200 else {}

@pytest.fixture(scope="module")
def probe():
    p = SystemTestProbe()
    p.connect()
    # Wait 12 seconds to let Sonarmock finish its startup sequence
    time.sleep(12)
    p.simulate_container_absent()
    time.sleep(1)
    yield p
    p.disconnect()

# =====================================================================
# PIANO DI TEST
# =====================================================================

def test_01_req1_richiesta_accettata(probe):
    state = probe.get_current_state()
    assert state.get("serviceState") == "disengaged"
    
    response = probe.send_load_request()
    assert response.status_code == 200
    
    data = response.json()
    assert data.get("status") == "accepted"
    
    new_state = probe.wait_for_state(lambda s: s.get("serviceState") == "engaged", timeout=5)
    assert new_state["serviceState"] == "engaged"

def test_02_req2_richiesta_rinviata_area_occupata(probe):
    probe.simulate_container_present()
    probe.wait_for_state(lambda s: s.get("ioPortOccupied") == True, timeout=10)
    
    response = probe.send_load_request()
    assert response.status_code == 200
    assert response.json().get("status") == "retrylater"

def test_03_req17_richieste_concorrenti_durante_operazione(probe):
    response = probe.send_load_request()
    assert response.json().get("status") == "retrylater"

def test_04_req6_deposito_entro_tempo_previsto(probe):
    state = probe.wait_for_state(lambda s: s.get("ioPortOccupied") == False, timeout=20)
    assert state["ioPortOccupied"] == False
    probe.simulate_container_absent()

def test_05_req12_workflow_completo_carico(probe):
    state = probe.wait_for_state(lambda s: s.get("serviceState") == "disengaged", timeout=60)
    assert state["serviceState"] == "disengaged"

def test_06_req18_nuova_richiesta_dopo_completamento(probe):
    response = probe.send_load_request()
    assert response.json().get("status") == "accepted"

def test_07_req8_ignorare_rilevamenti_temporanei(probe):
    probe.simulate_container_present()
    time.sleep(1) 
    probe.simulate_container_absent()
    time.sleep(2)
    state = probe.get_current_state()
    assert state.get("serviceState") == "engaged"

def test_08_req5_timeout_del_cliente(probe):
    state = probe.wait_for_state(lambda s: s.get("serviceState") == "disengaged", timeout=TIME_TIMEOUT_CLIENTE + 5)
    assert state["serviceState"] == "disengaged"

def test_09_req9_rilevamento_condizione_fuori_servizio(probe):
    probe.send_load_request()
    probe.simulate_container_present()
    probe.wait_for_state(lambda s: s.get("ioPortOccupied") == True, timeout=10)
    probe.wait_for_state(lambda s: s.get("ioPortOccupied") == False, timeout=20)
    time.sleep(1)
    
    probe.simulate_out_of_service() 
    
    state = probe.wait_for_state(lambda s: s.get("workingState") == "Out of service", timeout=TIME_STABLE_PRESENCE + 2)
    assert state["workingState"] == "Out of service"

def test_10_req3_richiesta_rinviata_servizio_non_disponibile(probe):
    response = probe.send_load_request()
    assert response.json().get("status") == "retrylater"

def test_11_req10_ignorare_anomalie_temporanee(probe):
    probe.simulate_container_absent()
    probe.wait_for_state(lambda s: s.get("workingState") == "Service working", timeout=TIME_STABLE_PRESENCE + 2)
    
    probe.simulate_out_of_service()
    time.sleep(1)
    probe.simulate_container_absent()
    
    time.sleep(3)
    state = probe.get_current_state()
    assert state.get("workingState") == "Service working"

def test_12_req11_ripristino_servizio(probe):
    probe.simulate_out_of_service()
    probe.wait_for_state(lambda s: s.get("workingState") == "Out of service", timeout=5)
    
    probe.simulate_container_absent()
    state = probe.wait_for_state(lambda s: s.get("workingState") == "Service working", timeout=5)
    assert state["workingState"] == "Service working"
    
    # Aspettiamo che il robot ritorni e diventi disengaged per i prossimi test
    probe.wait_for_state(lambda s: s.get("ioPortOccupied") == False, timeout=20)
    probe.simulate_container_absent()
    probe.wait_for_state(lambda s: s.get("serviceState") == "disengaged", timeout=60)

def test_13_req19_sequenza_riempimento_completo(probe):
    max_slots = 4
    for _ in range(max_slots):
        state = probe.get_current_state()
        all_occupied = all(v == "occupied" for k, v in state.get("slots", {}).items() if k != "slot5")
        if all_occupied:
            break
            
        resp = probe.send_load_request()
        if resp.json().get("status") == "accepted":
            probe.simulate_container_present()
            probe.wait_for_state(lambda s: s.get("ioPortOccupied") == True, timeout=10)
            probe.wait_for_state(lambda s: s.get("ioPortOccupied") == False, timeout=20)
            probe.simulate_container_absent()
            probe.wait_for_state(lambda s: s.get("serviceState") == "disengaged", timeout=60)
            
    state_before = probe.get_current_state()
    print(f"STATE AFTER LOOP, BEFORE FINAL LOAD REQUEST: {state_before}")
    resp = probe.send_load_request()
    assert resp.json().get("status") == "refused"
