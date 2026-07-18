import machine
import time
import network
from umqtt.simple import MQTTClient
from wifi_config import MQTT_BROKER, PASSWORD, SSID

# Configurazione unificata secondo il metamodello QAK
CLIENT_ID = 'esp32_sonar_node'
TOPIC_GLOBAL = b'cargosystem' # Topic centralizzato dichiarato nel file .qak

# Pins
trigger = machine.Pin(26, machine.Pin.OUT)
echo = machine.Pin(25, machine.Pin.IN)
led = machine.Pin(2, machine.Pin.OUT)

led_state = 'off'
msg_seq = 0  # Contatore sequenza messaggi QAK

def parse_qak_event(msg_str, event_id):
    """
    Parsifica un messaggio stringa in formato QAK ApplMessage.
    Sintassi standard: msg(MSGID, MSGTYPE, SENDER, RECEIVER, CONTENT, SEQNUM)
    Esempio atteso: msg(led_ctrl, event, ..., ..., ledCmd(CMD), ...)
    """
    try:
        if not msg_str.startswith("msg(") or not msg_str.endswith(")"):
            return None
        
        # Rimozione del wrapper msg(...)
        inner = msg_str[4:-1]
        tokens = inner.split(",")
        
        # Controllo che l'event ID corrisponda per evitare auto-ricezione del sonar
        if tokens[0].strip() != event_id:
            return None
            
        # Estrazione della sezione CONTENT (es. ledCmd(on))
        content = tokens[4].strip()
        if "(" in content and content.endswith(")"):
            payload = content[content.find("(")+1:-1]
            return payload
    except:
        pass
    return None

def sub_cb(topic, msg):
    global led_state
    msg_str = msg.decode('utf-8')
    
    # Estrae il valore dall'evento QAK 'led_ctrl'
    cmd = parse_qak_event(msg_str, "led_ctrl")
    
    if cmd is not None:
        if cmd == 'blink':
            led_state = 'blink'
        elif cmd == 'on':
            led_state = 'on'
            led.value(1)
        elif cmd == 'off':
            led_state = 'off'
            led.value(0)

def connect_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if not wlan.isconnected():
        wlan.connect(SSID, PASSWORD)
        while not wlan.isconnected():
            time.sleep(1)

def connect_mqtt():
    client = MQTTClient(CLIENT_ID, MQTT_BROKER)
    client.set_callback(sub_cb)
    client.connect()
    client.subscribe(TOPIC_GLOBAL) # Ascolta sullo stesso topic centralizzato
    return client

# Inizializzazione
connect_wifi()
client = connect_mqtt()

# Main Loop
while True:
    try:
        client.check_msg()
        
        # Gestione asincrona del lampeggio del LED
        if led_state == 'blink':
            led.value(not led.value())
            
        # Lettura del sensore a ultrasuoni
        trigger.value(0)
        time.sleep_us(5)
        trigger.value(1)
        time.sleep_us(10)
        trigger.value(0)
        
        duration = machine.time_pulse_us(echo, 1, 30000)
        if duration < 0:
            time.sleep(0.5)
            continue

        dist = int((duration * 0.0343) / 2)
        
        # Generazione dell'ApplMessage QAK per l'evento 'wall_sonardata'
        qak_msg = "msg(wall_sonardata,event,esp32_sonar,none,distance({}),{})".format(dist, msg_seq)
        msg_seq += 1
        
        # Pubblicazione sul canale centralizzato
        client.publish(TOPIC_GLOBAL, qak_msg.encode('utf-8'))
        
        time.sleep(0.5)
        
    except Exception:
        try:
            time.sleep(2)
            client = connect_mqtt()
        except:
            pass
