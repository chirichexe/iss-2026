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
    private CoapConnection currentConn = null;
    private CoapObserveRelation currentRel = null;

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
                        currentConn = (CoapConnection) conn;
                        System.out.println("CoapObserver | Attempting to observe coap://" + coapAddr + "/" + coapPath);
                        CoapObserveRelation rel = currentConn.observeResource(new CoapHandler() {
                            @Override
                            public void onLoad(CoapResponse response) {
                                active = true;
                                String content = response.getResponseText();
                                if (content != null && content.trim().startsWith("{") && !content.equals(wsController.getLastStateJson())) {
                                    System.out.println("CoapObserver | Resource update received via observe: " + content);
                                    wsController.broadcast(content);
                                }
                            }

                            @Override
                            public void onError() {
                                System.err.println("CoapObserver | Observation error or disconnection. Will reconnect...");
                                active = false;
                                currentConn = null;
                                currentRel = null;
                            }
                        });
                        currentRel = rel;
                        if (rel != null && !rel.isCanceled()) {
                            active = true;
                            try {
                                if (currentConn.getClient() != null) {
                                    CoapResponse resp = currentConn.getClient().get();
                                    if (resp != null) {
                                        String content = resp.getResponseText();
                                        if (content != null && content.trim().startsWith("{") && !content.equals(wsController.getLastStateJson())) {
                                            wsController.broadcast(content);
                                        }
                                    }
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    System.err.println("CoapObserver | Error setting up observe: " + e.getMessage());
                    active = false;
                    currentConn = null;
                    currentRel = null;
                }
            } else {
                if (currentConn != null && currentConn.getClient() != null) {
                    try {
                        CoapResponse resp = currentConn.getClient().get();
                        if (resp != null) {
                            String content = resp.getResponseText();
                            if (content != null && content.trim().startsWith("{") && !content.equals(wsController.getLastStateJson())) {
                                System.out.println("CoapObserver | Sync update detected via CoAP polling: " + content);
                                wsController.broadcast(content);
                            }
                        } else {
                            System.err.println("CoapObserver | CoAP health check returned null. Reconnecting...");
                            if (currentRel != null) currentRel.proactiveCancel();
                            active = false;
                            currentConn = null;
                            currentRel = null;
                        }
                    } catch (Exception e) {
                        System.err.println("CoapObserver | CoAP health check failed: " + e.getMessage());
                        if (currentRel != null) currentRel.proactiveCancel();
                        active = false;
                        currentConn = null;
                        currentRel = null;
                    }
                } else {
                    active = false;
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                break;
            }
        }
    }
}
