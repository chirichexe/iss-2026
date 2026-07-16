package unibo.basicomm23.examples.pingpong_dispatch;

import unibo.basicomm23.enablers.ServerFactory;
import unibo.basicomm23.examples.ActorNaiveCaller;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.IApplMsgHandler;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtilsOrig;

/*
PlayerCaller inizia il gioco : è il batsman
 */
public class BatsmanCaller extends ActorNaiveCaller {
    protected PlayerLogic playLogic = new PlayerLogic();
    protected ServerFactory server;

    public BatsmanCaller(String name, ProtocolType protocol, String hostAddr, String entry) {
        super(name, protocol, hostAddr, entry);
        startReceiver();
    }


    public void startReceiver(){
        IApplMsgHandler batsmanMsgHandler = new BatsmanMsgHandler(name+"Handler", this);
        server = new ServerFactory("server", 9797, protocol);
        server.addMsgHandler(batsmanMsgHandler);
        server.start();
        CommUtilsOrig.outred("BatsmanCaller receiver started on 9797 ");
    }

    protected void playUsingRequest(String destName, int i){
        String m = playLogic.hitBall();
        IApplMessage req = CommUtilsOrig.buildRequest( name, "hit", m, destName);
        CommUtilsOrig.outblue(name + " | sends: " + req);
        //request con timeout per simulare risposte errate
        try {
            IApplMessage answer;
            if( i < 3 ) answer = connSupport.request( req );  //Il receiver risponde bene
            else answer = connSupport.request(req, 300);  //Il receiver risponde male
            CommUtilsOrig.outblue(name + " | playUsingRequest answer=" + answer);
            playUsingRequest(destName,i+1);
        }catch( Exception e ){
            CommUtilsOrig.outred(name + " | playUsingRequest ERROR: " + e.getMessage());
        }
        CommUtilsOrig.delay(500);
    }
    public void playUsingDispatch( String destName ){
        try {
                 String m = playLogic.hitBall();
                IApplMessage hitanswer = CommUtilsOrig.buildDispatch(
                        name, "batsmanhit", m, destName);
                connSupport.forward(hitanswer);
                CommUtilsOrig.outblue(name + " | sends: " + hitanswer);
        }catch( Exception e ){
            CommUtilsOrig.outred(name + " | playUsingDispatch ERROR: " + e.getMessage());
        }
    }

    @Override
    public void body() throws Exception {
        String destName =  name.equals("ping") ? "pong" : "ping";
        //playUsingRequest(destName,0 );
        playUsingDispatch( destName );
    }
}
