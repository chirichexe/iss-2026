package it.unibo.guiserver;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

/**
 * SoC: Dedicated MQTT observer for domain state updates.
 * Subscribes to 'cargosystem' and 'cargoservice/status' topics on tcp://localhost:1883
 * to deliver instant domain state changes to WebSocket connected clients alongside CoAP.
 */
public class MqttObserver implements Runnable {
    private final WsController wsController;
    private final String brokerUrl = "tcp://localhost:1883";
    private volatile boolean running = true;

    public MqttObserver(WsController wsController) {
        this.wsController = wsController;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running) {
            try {
                MqttClient client = new MqttClient(brokerUrl, "GuiServer_MqttObserver_" + System.currentTimeMillis(), new MemoryPersistence());
                MqttConnectOptions options = new MqttConnectOptions();
                options.setAutomaticReconnect(true);
                options.setCleanSession(true);
                options.setConnectionTimeout(5);
                
                client.setCallback(new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        System.err.println("MqttObserver | Connection lost: " + (cause != null ? cause.getMessage() : "unknown"));
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        String content = new String(message.getPayload());
                        if (content != null && content.trim().startsWith("{")) {
                            wsController.broadcast(content);
                        }
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {}
                });

                System.out.println("MqttObserver | Connecting to broker " + brokerUrl + "...");
                client.connect(options);
                client.subscribe("cargoservice/status", 1);
                System.out.println("MqttObserver | Subscribed to 'cargoservice/status'.");

                while (running && client.isConnected()) {
                    Thread.sleep(1000);
                }
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
            } catch (Exception e) {
                System.err.println("MqttObserver | Error or disconnection: " + e.getMessage() + ". Retrying in 3s...");
                try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            }
        }
    }
}
