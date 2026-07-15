package it.unibo.guiserver;

import io.javalin.http.Context;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

/**
 * SoC: Dedicated controller for short HTTP REST transactions.
 * Acts as an Inbound Adapter translating HTTP POST requests from the browser
 * into formal QAK requests (load_request) sent over TCP/MQTT to the domain actor (cargoservice).
 */
public class HttpController {
    private final GuiServerQakActor guiActor;
    private final String targetHost = "127.0.0.1";
    private final String targetPort = "8050";

    public HttpController(GuiServerQakActor guiActor) {
        this.guiActor = guiActor;
    }

    public void handleLoadRequest(Context ctx) {
        System.out.println("HttpController | Received POST /api/load from GUI");
        try {
            Interaction tcpConn = ConnectionFactory.createClientSupport23(ProtocolType.tcp, targetHost, targetPort);
            if (tcpConn != null) {
                try {
                    // Notify the formal QAK actor of user interaction (Inbound Adapter translation)
                    if (guiActor != null) {
                        guiActor.notifyUserAction("load_request");
                    }

                    IApplMessage reqMsg = CommUtils.buildRequest("ioportgui", "load_request", "loadRequest(none)", "cargoservice");
                    System.out.println("HttpController | Forwarding formal QAK request to cargoservice: " + reqMsg);
                    IApplMessage replyMsg = tcpConn.request(reqMsg);
                    System.out.println("HttpController | cargoservice reply: " + replyMsg);

                    String msgId = replyMsg.msgId();
                    if ("load_accepted".equals(msgId)) {
                        String content = replyMsg.msgContent(); // e.g., loadAccepted(slot1)
                        String slotName = "slot1";
                        if (content.contains("(") && content.contains(")")) {
                            slotName = content.substring(content.indexOf('(') + 1, content.indexOf(')'));
                        }
                        ctx.contentType("application/json").result("{\"status\":\"accepted\",\"slot\":\"" + slotName + "\"}");
                    } else if ("load_retrylater".equals(msgId)) {
                        ctx.contentType("application/json").result("{\"status\":\"retrylater\"}");
                    } else if ("load_refused".equals(msgId)) {
                        ctx.contentType("application/json").result("{\"status\":\"refused\"}");
                    } else {
                        ctx.status(500).contentType("application/json")
                           .result("{\"status\":\"error\",\"message\":\"Unexpected reply: " + msgId + "\"}");
                    }
                } finally {
                    try { tcpConn.close(); } catch (Exception e) { /* ignore */ }
                }
            } else {
                System.err.println("HttpController | Failed to connect to cargoservice on port " + targetPort);
                ctx.status(503).contentType("application/json").result("{\"status\":\"retrylater\",\"message\":\"cargoservice unreachable\"}");
            }
        } catch (Exception e) {
            System.err.println("HttpController | Error processing load request: " + e.getMessage());
            ctx.status(500).contentType("application/json").result("{\"status\":\"error\",\"message\":\"" + (e.getMessage() != null ? e.getMessage() : "Unknown error") + "\"}");
        }
    }
}
