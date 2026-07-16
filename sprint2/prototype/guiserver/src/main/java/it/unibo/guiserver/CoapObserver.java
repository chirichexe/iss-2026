package it.unibo.guiserver;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;

/**
 * Monitors the cargoservice domain state via CoAP Observe (RFC 7641 push),
 * mirroring the pattern used in the reference sprint3 (CargoserviceCoAPObserver).
 * When the QAK actor calls updateResource(), the server pushes a notification here
 * which is then broadcast to all connected WebSocket clients.
 */
public class CoapObserver {
    private final WsController wsController;
    private final String coapUrl = "coap://127.0.0.1:8050/ctxcargoservice/cargoservice";
    private CoapClient client;
    private CoapObserveRelation relation;

    public CoapObserver(WsController wsController) {
        this.wsController = wsController;
        this.client = new CoapClient(coapUrl);

        System.out.println("CoapObserver | Registering CoAP Observe on: " + coapUrl);

        relation = client.observe(new CoapHandler() {
            @Override
            public void onLoad(CoapResponse response) {
                processAndBroadcast(response.getResponseText());
            }

            @Override
            public void onError() {
                System.err.println("CoapObserver | Errore osservando la risorsa CoAP (server irraggiungibile o relazione persa).");
            }
        });
    }

    public void stop() {
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

        // Il QAK può wrappare il payload in un ApplMessage: estraiamo solo il JSON { ... }
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
