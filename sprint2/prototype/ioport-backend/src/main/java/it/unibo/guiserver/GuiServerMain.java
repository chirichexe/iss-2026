package it.unibo.guiserver;

/**
 * Main entrypoint for the standalone external Web GUI Server (guiserver26qak0).
 * Implements the Inbound Adapter pattern with strict Separation of Concerns (SoC).
 */
public class GuiServerMain {

    public static void main(String[] args) {
        System.out.println("=========================================================================");
        System.out.println("GuiServerMain | Starting External Web Server (guiserver26qak0)...        ");
        System.out.println("=========================================================================");

        int port = 8086;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        // 1. Initialize specialized SoC Controllers
        WsController wsController = new WsController();
        HttpController httpController = new HttpController(wsController);

        // 2. Initialize and start the internal Javalin GUI Handler
        JavalinGuiHandler guiHandler = new JavalinGuiHandler(port, httpController, wsController);
        guiHandler.start();

        // 3. Register CoAP Observe (push) to stream domain state updates to WebSocket clients
        CoapObserver coapObserver = new CoapObserver(wsController);
    }
}
