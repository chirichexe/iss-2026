package it.unibo.ioport

import io.javalin.Javalin
import io.javalin.http.staticfiles.Location
import io.javalin.websocket.WsContext
import unibo.basicomm23.coap.CoapConnection
import unibo.basicomm23.interfaces.Interaction
import unibo.basicomm23.interfaces.IApplMessage
import unibo.basicomm23.msg.ApplMessage
import unibo.basicomm23.msg.ProtocolType
import unibo.basicomm23.utils.CommUtils
import unibo.basicomm23.utils.ConnectionFactory
import org.eclipse.californium.core.CoapHandler
import org.eclipse.californium.core.CoapResponse
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

object IOPortServer {
    private val wsSessions = ConcurrentHashMap.newKeySet<WsContext>()
    private var lastStateJson: String = "{\"serviceState\":\"disengaged\",\"workingState\":\"Service working\",\"ioPortOccupied\":false,\"reservedSlot\":-1,\"slots\":{\"slot1\":\"free\",\"slot2\":\"free\",\"slot3\":\"free\",\"slot4\":\"free\",\"slot5\":\"marker\"}}"

    @JvmStatic
    fun main(args: Array<String>) {
        println("=========================================================================")
        println("IOPortServer | Starting Web Facade Server for Maritime CargoService...")
        println("=========================================================================")

        var webDir = File("../ioport-frontend/web/ioport")
        if (!webDir.exists()) {
            webDir = File("web/ioport")
        }
        val absWebDir = if (webDir.exists()) webDir.absolutePath else "web/ioport"
        println("IOPortServer | Serving static web files from: $absWebDir")

        val port = args.firstOrNull()?.toIntOrNull() ?: 8086
        val app = Javalin.create { config ->
            config.staticFiles.add(absWebDir, Location.EXTERNAL)
        }.start(port)

        println("IOPortServer | HTTP & WebSocket server started on http://localhost:$port")

        app.ws("/ws") { ws ->
            ws.onConnect { ctx ->
                println("IOPortServer | WebSocket connected: ${ctx.sessionId}")
                wsSessions.add(ctx)
                ctx.send(lastStateJson)
            }
            ws.onClose { ctx ->
                println("IOPortServer | WebSocket closed: ${ctx.sessionId}")
                wsSessions.remove(ctx)
            }
            ws.onError { ctx ->
                wsSessions.remove(ctx)
            }
        }

        app.post("/api/load") { ctx ->
            println("IOPortServer | Received POST /api/load from GUI")
            try {
                val tcpConn: Interaction? = ConnectionFactory.createClientSupport23(ProtocolType.tcp, "127.0.0.1", "8050")
                if (tcpConn != null) {
                    try {
                        val reqMsg: IApplMessage = CommUtils.buildRequest("ioportgui", "load_request", "loadRequest(none)", "cargoservice")
                        println("IOPortServer | Forwarding request to cargoservice: $reqMsg")
                        val replyMsg: IApplMessage = tcpConn.request(reqMsg)
                        println("IOPortServer | cargoservice reply: $replyMsg")

                        when (replyMsg.msgId()) {
                            "load_accepted" -> {
                                val arg = replyMsg.msgContent() // e.g. loadAccepted(slot1)
                                val slotName = if (arg.contains("(")) arg.substringAfter("(").substringBefore(")") else "slot1"
                                ctx.contentType("application/json").result("{\"status\":\"accepted\",\"slot\":\"$slotName\"}")
                            }
                            "load_retrylater" -> {
                                ctx.contentType("application/json").result("{\"status\":\"retrylater\"}")
                            }
                            "load_refused" -> {
                                ctx.contentType("application/json").result("{\"status\":\"refused\"}")
                            }
                            else -> {
                                ctx.status(500).contentType("application/json").result("{\"status\":\"error\",\"message\":\"Unexpected reply: ${replyMsg.msgId()}\"}")
                            }
                        }
                    } finally {
                        try { tcpConn.close() } catch (e: Exception) { /* ignore */ }
                    }
                } else {
                    println("IOPortServer | Failed to connect to cargoservice on port 8050")
                    ctx.status(503).contentType("application/json").result("{\"status\":\"retrylater\",\"message\":\"cargoservice unreachable\"}")
                }
            } catch (e: Exception) {
                println("IOPortServer | Error processing load request: ${e.message}")
                ctx.status(500).contentType("application/json").result("{\"status\":\"error\",\"message\":\"${e.message ?: "Unknown error"}\"}")
            }
        }

        app.get("/api/state") { ctx ->
            ctx.contentType("application/json").result(lastStateJson)
        }

        thread(start = true, isDaemon = true, name = "CoapObserverThread") {
            var currentConn: CoapConnection? = null
            var currentRel: org.eclipse.californium.core.CoapObserveRelation? = null
            while (true) {
                if (currentRel == null || currentRel.isCanceled) {
                    try {
                        val conn = ConnectionFactory.createClientSupport23(ProtocolType.coap, "127.0.0.1:8050", "ctxcargoservice/cargoservice")
                        if (conn != null && conn is CoapConnection) {
                            currentConn = conn
                            println("IOPortServer | Attempting to observe coap://127.0.0.1:8050/ctxcargoservice/cargoservice")
                            val observeRel = conn.getClient().observe(object : CoapHandler {
                                override fun onLoad(response: CoapResponse) {
                                    val content = response.responseText
                                    if (content != null && content.trim().startsWith("{") && content != lastStateJson) {
                                        println("IOPortServer | CoAP resource update received: $content")
                                        lastStateJson = content
                                        wsSessions.forEach { wsCtx ->
                                            try {
                                                if (wsCtx.session.isOpen) wsCtx.send(content)
                                            } catch (e: Exception) {
                                                println("IOPortServer | Error broadcasting to ws: ${e.message}")
                                            }
                                        }
                                    }
                                }
                                override fun onError() {
                                    println("IOPortServer | CoAP observation error or disconnect. Reconnecting...")
                                    currentConn = null
                                    currentRel = null
                                }
                            })
                            currentRel = observeRel
                        }
                    } catch (e: Exception) {
                        println("IOPortServer | Error setting up observe: ${e.message}")
                        currentConn = null
                        currentRel = null
                    }
                }
                try {
                    Thread.sleep(2000)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }
}
