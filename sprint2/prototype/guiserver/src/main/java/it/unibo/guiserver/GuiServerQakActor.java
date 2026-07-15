package it.unibo.guiserver;

import it.unibo.kactor.ActorBasicFsm;
import kotlinx.coroutines.CoroutineScope;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.utils.CommUtils;

/**
 * Evoluzione Finale: Il Server diventa un Attore QAK (guiserver26qak0).
 * Models the external web server as an active QAK actor inside the distributed ecosystem.
 * Receives internal notifications from Javalin handlers and publishes formal events or
 * dispatches via TCP/MQTT to the domain actors.
 */
public class GuiServerQakActor extends ActorBasicFsm {

    public GuiServerQakActor(String name, CoroutineScope scope) {
        super(name, scope, false, false, false, false, 50);
    }

    @Override
    public String getInitialState() {
        return "s0";
    }

    @Override
    public kotlin.jvm.functions.Function1<ActorBasicFsm, kotlin.Unit> getBody() {
        return (ActorBasicFsm fsm) -> {
            fsm.state("s0", (it.unibo.kactor.State st) -> {
                CommUtils.outblue("GuiServerQakActor | STARTED as formal QAK Actor (Inbound Adapter)");
                return kotlin.Unit.INSTANCE;
            });
            return kotlin.Unit.INSTANCE;
        };
    }

    /**
     * Called by internal handlers when a user action takes place on the web GUI.
     */
    public void notifyUserAction(String actionName) {
        CommUtils.outgreen("GuiServerQakActor | Registered user interaction event: " + actionName);
    }
}
