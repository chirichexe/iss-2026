import machine
import network
import time
from umqtt.simple import MQTTClient
from wifi_config import MQTT_BROKER, PASSWORD, SSID

# Configurazione applicativa
CLIENT_ID = 'esp32_sonar'
TOPIC_PUB = b'sensore/sonar'
TOPIC_SUB = b'comando/led'

# Pin
trigger = machine.Pin(26, machine.Pin.OUT)
echo = machine.Pin(25, machine.Pin.IN)
led = machine.Pin(2, machine.Pin.OUT) # LED integrato

def sub_cb(topic, msg):
    print("Messaggio ricevuto:", msg)
    if msg == b'blink':
        for _ in range(5):
            led.value(1)
            time.sleep(0.2)
            led.value(0)
            time.sleep(0.2)

def connect_wifi():
    wlan = network.WLAN(network.STA_IF)
    wlan.active(True)
    if not wlan.isconnected():
        wlan.connect(SSID, PASSWORD)
        while not wlan.isconnected():
            time.sleep(1)

# Inizializzazione
connect_wifi()
client = MQTTClient(CLIENT_ID, MQTT_BROKER)
client.set_callback(sub_cb)
client.connect()
client.subscribe(TOPIC_SUB)
print("MQTT Connesso e in ascolto su", TOPIC_SUB)

try:
    while True:
        # Controlla messaggi in arrivo (non bloccante)
        client.check_msg()
        
        # Logica sonar
        trigger.value(0)
        time.sleep_us(5)
        trigger.value(1)
        time.sleep_us(10)
        trigger.value(0)
        duration = machine.time_pulse_us(echo, 1, 30000)
        dist = (duration * 0.0343) / 2 if duration >= 0 else 0
        
        client.publish(TOPIC_PUB, str(dist))
        print("Distanza:", dist)
        
        time.sleep(1)
except Exception as e:
    print("Errore:", e)
