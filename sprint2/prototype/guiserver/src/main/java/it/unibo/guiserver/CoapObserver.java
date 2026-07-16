package it.unibo.guiserver;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapResponse;

/**
 * SoC: Dedicated component for monitoring the domain state via CoAP.
 * Note: Implements an optimized "Smart Polling" pattern because the current
 * server-side QAK framework does not natively trigger pure-push Observe notifications.
 */
public class CoapObserver implements Runnable {
    private final WsController wsController;
    private final String coapUrl = "coap://127.0.0.1:8050/ctxcargoservice/cargoservice";
    private volatile boolean running = true;
    private CoapClient client;

    public CoapObserver(WsController wsController) {
        this.wsController = wsController;
    }

    public void stop() {
        this.running = false;
        if (client != null) {
            client.shutdown();
        }
    }

    @Override
    public void run() {
        System.out.println("CoapObserver | Starting Smart Polling on: " + coapUrl);
        client = new CoapClient(coapUrl);
        client.setTimeout(2000L); // Timeout basso per non bloccare il thread

        while (running) {
            try {
                CoapResponse resp = client.get();
                if (resp != null) {
                    processAndBroadcast(resp.getResponseText());
                } else {
                    System.err.println("CoapObserver | Server non raggiungibile. Ritento...");
                }
            } catch (Exception e) {
                System.err.println("CoapObserver | Errore durante il polling: " + e.getMessage());
            }

            try {
                // Polling frequente (500ms) per un'esperienza real-time fluida
                Thread.sleep(500);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    private void processAndBroadcast(String rawContent) {
        if (rawContent == null || rawContent.trim().isEmpty()) return;

        String jsonPayload = rawContent.trim();

        // Estrazione sicura: se il QAK wrappa il payload in un ApplMessage,
        // preleviamo forzatamente solo la parte JSON { ... }
        if (jsonPayload.contains("{") && jsonPayload.contains("}")) {
            int firstBrace = jsonPayload.indexOf('{');
            int lastBrace = jsonPayload.lastIndexOf('}');
            if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
                jsonPayload = jsonPayload.substring(firstBrace, lastBrace + 1);
            }
        }

        // Se è un JSON valido ed è DIVERSO dall'ultimo inviato, esegui il broadcast
        if (jsonPayload.startsWith("{") && !jsonPayload.equals(wsController.getLastStateJson())) {
            // System.out.println("CoapObserver | Stato aggiornato rilevato: " + jsonPayload);
            wsController.broadcast(jsonPayload);
        }
    }
}
