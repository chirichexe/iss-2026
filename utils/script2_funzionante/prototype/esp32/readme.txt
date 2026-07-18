1. Caricamento Firmware (Recovery)

In caso di corruzione del firmware o boot-loop (disconnessioni cicliche):

    Cancellazione flash: esptool --port /dev/ttyUSB0 erase-flash
    Flash nuovo firmware: esptool --port /dev/ttyUSB0 write-flash 0x1000 NOME_FILE.bin 
    Se la porta seriale si disconnette durante il processo, tieni premuto il tasto BOOT fisico sulla scheda ESP32 durante l'esecuzione del comando.

2. Aggiornamento Script

Per aggiornare il codice senza dover reinstallare il firmware:

    - Assicurati che lo script locale sia salvato come main.py.
    - Crea wifi_config.py partendo da wifi_config.example.py e inserisci SSID, password e IP del broker MQTT.
      Il file wifi_config.py e' ignorato da Git.

    - Sovrascrivi con rshell:
    rshell -p /dev/ttyUSB0 cp main.py /pyboard/main.py
    rshell -p /dev/ttyUSB0 cp wifi_config.py /pyboard/wifi_config.py

3. Ascoltare cose MQTT
    blink: docker exec -it mosquitto mosquitto_pub -t comando/led -m "blink"
    leggi dati: docker exec -it mosquitto mosquitto_sub -t sensore/sonar
