import time
import threading
import paho.mqtt.client as mqtt

# Configurazione unificata secondo il metamodello QAK
MQTT_BROKER = "localhost" # Modifica se il broker è su un'altra macchina
TOPIC_GLOBAL = "cargosystem"
CLIENT_ID = "esp32_sonar_simulator"

led_state = 'off'
msg_seq = 0
current_dist = 60 # Distanza di default (sicura)

def parse_qak_event(msg_str, event_id):
    """
    Parsifica un messaggio stringa in formato QAK ApplMessage.
    Sintassi standard: msg(MSGID, MSGTYPE, SENDER, RECEIVER, CONTENT, SEQNUM)
    """
    try:
        if not msg_str.startswith("msg(") or not msg_str.endswith(")"):
            return None
        
        inner = msg_str[4:-1]
        tokens = inner.split(",")
        
        if tokens[0].strip() != event_id:
            return None
            
        content = tokens[4].strip()
        if "(" in content and content.endswith(")"):
            payload = content[content.find("(")+1:-1]
            return payload
    except:
        pass
    return None

def on_message(client, userdata, msg):
    global led_state
    msg_str = msg.payload.decode('utf-8')
    
    # Estrae il valore dall'evento QAK 'led_event'
    cmd = parse_qak_event(msg_str, "led_event")
    
    if cmd is not None:
        if cmd == 'blink':
            if led_state != 'blink':
                print(f"\n[LED] Stato aggiornato: LAMPEGGIO (blink)")
            led_state = 'blink'
        elif cmd == 'on':
            if led_state != 'on':
                print(f"\n[LED] Stato aggiornato: ACCESO (on)")
            led_state = 'on'
        elif cmd == 'off':
            if led_state != 'off':
                print(f"\n[LED] Stato aggiornato: SPENTO (off)")
            led_state = 'off'

def on_connect(client, userdata, flags, rc):
    print(f"Connesso al broker MQTT {MQTT_BROKER} (Codice: {rc})")
    client.subscribe(TOPIC_GLOBAL)

def input_thread():
    """Thread per permettere di variare la distanza simulata in tempo reale"""
    global current_dist
    while True:
        try:
            val = input()
            current_dist = int(val)
            print(f"[SONAR] Distanza impostata a {current_dist}")
        except ValueError:
            print("Inserisci un numero intero valido.")
        except Exception:
            break

# Inizializzazione Client MQTT (paho-mqtt)
client = mqtt.Client(client_id=CLIENT_ID)
client.on_connect = on_connect
client.on_message = on_message

try:
    client.connect(MQTT_BROKER, 1883, 60)
except Exception as e:
    print(f"Errore di connessione MQTT: {e}")
    print("Assicurati di avere paho-mqtt installato (pip install paho-mqtt) e mosquitto in esecuzione.")
    exit(1)

# Avvia il thread di background per la gestione asincrona dei messaggi MQTT
client.loop_start()

# Avvia thread per input da tastiera
threading.Thread(target=input_thread, daemon=True).start()

print("=====================================================")
print(" SIMULATORE ESP32 (Sonar + LED)")
print("=====================================================")
print(" Digita un numero e premi INVIO per cambiare la distanza del sonar in tempo reale.")
print("=====================================================")

# Main Loop (simile a quello dell'ESP32 reale)
try:
    while True:
        # Generazione dell'ApplMessage QAK per l'evento 'wall_sonardata'
        qak_msg = "msg(wall_sonardata,event,esp32_sonar,none,distance({}),{})".format(current_dist, msg_seq)
        msg_seq += 1
        
        # Pubblicazione sul canale centralizzato
        client.publish(TOPIC_GLOBAL, qak_msg)
        
        time.sleep(0.5)
except KeyboardInterrupt:
    print("\nChiusura simulatore in corso...")
    client.loop_stop()
    client.disconnect()
