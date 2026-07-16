import machine
import time
import network
from umqtt.simple import MQTTClient
from wifi_config import MQTT_BROKER, PASSWORD, SSID

# config
CLIENT_ID = 'esp32_sonar_node'
TOPIC_PUB = b'sensore/sonar'
TOPIC_SUB = b'comando/led'

# pins
trigger = machine.Pin(26, machine.Pin.OUT)
echo = machine.Pin(25, machine.Pin.IN)
led = machine.Pin(2, machine.Pin.OUT)

def sub_cb(topic, msg):
    if msg == b'blink':
        for _ in range(5):
            led.value(1)
            time.sleep(0.2)
            led.value(0)
            time.sleep(0.2)
    elif msg == b'on':
        led.value(1)
    elif msg == b'off':
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
    client.subscribe(TOPIC_SUB)
    return client

#init
connect_wifi()
client = connect_mqtt()

# main loop
while True:
    try:
        client.check_msg()
        
        trigger.value(0)
        time.sleep_us(5)
        trigger.value(1)
        time.sleep_us(10)
        trigger.value(0)
        
        duration = machine.time_pulse_us(echo, 1, 30000)
        dist = (duration * 0.0343) / 2 if duration >= 0 else 0
        
        # publish data
        client.publish(TOPIC_PUB, str(dist))
        
        time.sleep(0.5)
        
    except Exception:
        try:
            time.sleep(2)
            client = connect_mqtt()
        except:
            pass
