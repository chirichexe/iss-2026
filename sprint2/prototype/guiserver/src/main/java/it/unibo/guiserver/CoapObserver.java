package it.unibo.guiserver;

import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import unibo.basicomm23.coap.CoapConnection;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.ConnectionFactory;

/**
 * SoC: Dedicated component for observing the domain state via CoAP.
 * Connects to coap://127.0.0.1:8050/ctxcargoservice/cargoservice and triggers
 * WebSocket broadcasts upon receiving resource state updates.
 * Implements continuous retry and auto-reconnect on observation failure.
 */
public class CoapObserver implements Runnable {
    private final WsController wsController;
    private final String coapAddr = "127.0.0.1:8050";
    private final String coapPath = "ctxcargoservice/cargoservice";
    private volatile boolean running = true;
    private volatile boolean active = false;

    public CoapObserver(WsController wsController) {
        this.wsController = wsController;
    }

    public void stop() {
        this.running = false;
    }

    @Override
    public void run() {
        while (running) {
            if (!active) {
                try {
                    Interaction conn = ConnectionFactory.createClientSupport23(ProtocolType.coap, coapAddr, coapPath);
                    if (conn instanceof CoapConnection) {
                        CoapConnection coapConn = (CoapConnection) conn;
                        System.out.println("CoapObserver | Attempting to observe coap://" + coapAddr + "/" + coapPath);
                        CoapObserveRelation rel = coapConn.observeResource(new CoapHandler() {
                            @Override
                            public void onLoad(CoapResponse response) {
                                active = true;
                                String content = response.getResponseText();
                                System.out.println("CoapObserver | Resource update received: " + content);
                                wsController.broadcast(content);
                            }

                            @Override
                            public void onError() {
                                System.err.println("CoapObserver | Observation error or disconnection. Will reconnect in 2s...");
                                active = false;
                            }
                        });
                        if (rel != null && !rel.isCanceled()) {
                            active = true;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("CoapObserver | Error setting up observe: " + e.getMessage());
                    active = false;
                }
            }
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ie) {
                break;
            }
        }
    }
}
