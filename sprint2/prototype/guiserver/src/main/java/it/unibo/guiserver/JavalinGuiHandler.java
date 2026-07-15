package it.unibo.guiserver;

import java.io.File;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;

/**
 * SoC: Internal handler responsible for wiring Javalin web routes and serving static files.
 * Delegates HTTP requests to HttpController and WebSocket events to WsController.
 */
public class JavalinGuiHandler {
    private final int port;
    private final HttpController httpController;
    private final WsController wsController;
    private Javalin app;

    public JavalinGuiHandler(int port, HttpController httpController, WsController wsController) {
        this.port = port;
        this.httpController = httpController;
        this.wsController = wsController;
    }

    public void start() {
        File webDir = new File("web/ioport");
        String absWebDir = webDir.exists() ? webDir.getAbsolutePath() : "web/ioport";
        System.out.println("JavalinGuiHandler | Serving static web files from: " + absWebDir);

        app = Javalin.create(config -> {
            config.staticFiles.add(absWebDir, Location.EXTERNAL);
        }).start(port);

        System.out.println("JavalinGuiHandler | HTTP & WebSocket server started on http://localhost:" + port);

        // Wiring WebSocket routes
        app.ws("/ws", ws -> {
            ws.onConnect(wsController::onConnect);
            ws.onClose(wsController::onClose);
            ws.onError(wsController::onError);
        });

        // Wiring HTTP routes
        app.post("/api/load", httpController::handleLoadRequest);
    }

    public void stop() {
        if (app != null) {
            app.stop();
        }
    }
}
