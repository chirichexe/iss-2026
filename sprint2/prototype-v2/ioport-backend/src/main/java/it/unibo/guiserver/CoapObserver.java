package it.unibo.guiserver;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;

/**
 * Monitors the cargoservice domain state via CoAP Observe (RFC 7641 push),
 * mirroring the pattern used in the reference sprint3 (CargoserviceCoAPObserver).
 * Uses a background thread to automatically reconnect if the target service is
 * not yet online or goes down during execution.
 */
public class CoapObserver {
    private final WsController wsController;
    private final String coapUrl = "coap://127.0.0.1:8050/ctxcargoservice/cargoservice";
    private CoapClient client;
    private CoapObserveRelation relation;
    private volatile boolean running = true;
    private Thread observerThread;

    public CoapObserver(WsController wsController) {
        this.wsController = wsController;
        this.client = new CoapClient(coapUrl);

        System.out.println("CoapObserver | Starting CoAP Reconnecting Observer on: " + coapUrl);

        observerThread = new Thread(() -> {
            while (running) {
                if (relation == null || relation.isCanceled()) {
                    try {
                        relation = client.observe(new CoapHandler() {
                            @Override
                            public void onLoad(CoapResponse response) {
                                processAndBroadcast(response.getResponseText());
                            }

                            @Override
                            public void onError() {
                                System.err.println("CoapObserver | CoAP observation error. Will reconnect...");
                                relation = null;
                            }
                        });
                    } catch (Exception e) {
                        System.err.println("CoapObserver | Exception during observe: " + e.getMessage());
                        relation = null;
                    }
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        observerThread.setDaemon(true);
        observerThread.start();
    }

    public void stop() {
        running = false;
        if (observerThread != null) {
            observerThread.interrupt();
        }
        if (relation != null) {
            relation.proactiveCancel();
        }
        if (client != null) {
            client.shutdown();
        }
    }

    private void processAndBroadcast(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) return;

        String jsonPayload = rawContent.trim();

        // QAK might wrap the payload in an ApplMessage, extract only the inner JSON { ... }
        if (jsonPayload.contains("{") && jsonPayload.contains("}")) {
            int firstBrace = jsonPayload.indexOf('{');
            int lastBrace  = jsonPayload.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                jsonPayload = jsonPayload.substring(firstBrace, lastBrace + 1);
            }
        }

        if (jsonPayload.startsWith("{")) {
            System.out.println("CoapObserver | Push ricevuto: " + jsonPayload);
            wsController.broadcast(jsonPayload);
        }
    }
}
