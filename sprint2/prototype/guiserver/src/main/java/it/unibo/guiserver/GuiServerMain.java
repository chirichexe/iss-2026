package it.unibo.guiserver;

import kotlinx.coroutines.GlobalScope;

/**
 * Main entrypoint for the standalone external Web GUI Server (guiserver26qak0).
 * Implements the Inbound Adapter pattern with strict Separation of Concerns (SoC).
 */
public class GuiServerMain {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println("GuiServerMain | Starting External Web Facade Server (guiserver26qak0)...");
        System.out.println("=========================================================================");

        int port = 8086;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        // 1. Initialize formal QAK Actor representing the web server
        GuiServerQakActor guiActor = new GuiServerQakActor("guiserver26qak0", GlobalScope.INSTANCE);

        // 2. Initialize specialized SoC Controllers
        WsController wsController = new WsController();
        HttpController httpController = new HttpController(guiActor, wsController);

        // 3. Initialize and start the internal Javalin GUI Handler
        JavalinGuiHandler guiHandler = new JavalinGuiHandler(port, httpController, wsController);
        guiHandler.start();

        // 4. Start CoAP Observer thread to stream domain state updates to WebSocket clients (MQTT observer removed)
        CoapObserver coapObserver = new CoapObserver(wsController);
        Thread coapThread = new Thread(coapObserver, "CoapObserverThread");
        coapThread.setDaemon(true);
        coapThread.start();
    }
}
