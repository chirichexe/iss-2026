package it.unibo.guiserver;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import io.javalin.websocket.WsContext;

/**
 * SoC: Dedicated controller for persistent WebSocket sessions.
 * Separates long-lived real-time connections from short HTTP requests.
 */
public class WsController {
    private final Set<WsContext> sessions = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private String lastStateJson = "{\"serviceState\":\"disengaged\",\"workingState\":\"Service working\",\"ioPortOccupied\":false,\"reservedSlot\":-1,\"slots\":{\"slot1\":\"free\",\"slot2\":\"free\",\"slot3\":\"free\",\"slot4\":\"free\",\"slot5\":\"marker\"}}";

    public void onConnect(WsContext ctx) {
        System.out.println("WsController | WebSocket connected: " + ctx.getSessionId());
        sessions.add(ctx);
        ctx.send(lastStateJson);
    }

    public void onClose(WsContext ctx) {
        System.out.println("WsController | WebSocket closed: " + ctx.getSessionId());
        sessions.remove(ctx);
    }

    public void onError(WsContext ctx) {
        System.out.println("WsController | WebSocket error on session: " + ctx.getSessionId());
        sessions.remove(ctx);
    }

    public synchronized void broadcast(String stateJson) {
        if (stateJson != null && stateJson.trim().startsWith("{")) {
            this.lastStateJson = stateJson;
            for (WsContext ctx : sessions) {
                try {
                    if (ctx.session.isOpen()) {
                        ctx.send(stateJson);
                    }
                } catch (Exception e) {
                    System.err.println("WsController | Error broadcasting to session " + ctx.getSessionId() + ": " + e.getMessage());
                }
            }
        }
    }

    public String getLastStateJson() {
        return lastStateJson;
    }
}
