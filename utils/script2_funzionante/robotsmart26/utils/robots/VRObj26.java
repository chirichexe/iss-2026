package robots;
import java.util.Observable;
import org.json.simple.JSONObject;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.IObserver;
import unibo.basicomm23.interfaces.IObserverMsg;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils; 
import unibo.basicomm23.utils.ConnectionFactory;
import unibo.basicomm23.utils.LogUtils;
import unibo.basicomm23.ws.WsConnection;

/*
 * -------------------------------------------------------------------------------
 * VRObj26
 * 
 * Un POJO che nasconde la comunicazione con il VirtualRobot26
 * offrendo un insieme di metodi per l'uso del DRR, definiti dalla 
 * interface IRobotBasicMoves
 * 
 * Questa versione usa il VirtualRobot26. 
 * La classe WsconnObserver apre una connessione (conn) via WebSocket sulla porta 8091
 * ed opera come  OBSERVER su questa connessione,
 * trasformando le informazioni ricevute in chiamate ai metodi di VRObj26
 * che realizzano le IRobotBasicMoves.
 * 
 * TRASFORMA le informazioni ricevute su wsconn in eventi
 *      sonardata : sonar(D)
 *      vrinfo    : vrinfo(A,B)  //MOVE,ENDMOVE | elapsed,collision | obstacle,unknown
 *      stepFail  : stepFail(TIMEOK)		
 *
 * ------------------------------------------------------------------------
*/
public class VRObj26  implements IRobotBasicMoves { 
    protected Interaction conn;
	protected WsconnObserver wsconn;
    protected int elapsed             = 0;      
    protected String asynchMoveResult = null;  
    protected int threadCount = 1;
    protected String toApplMsg   ;
    protected boolean tracing         = false;
    protected boolean doingStepSynch  = false;
    protected boolean doingStepAsynch = false;
    protected LogUtils logger         = new LogUtils("robobj");
    protected String LogFName         = null;
    protected IObserverMsg observerHl;

    //Factory method
    public static VRObj26 create(  IObserverMsg observer, String LogFName ) {
    	return new VRObj26(  observer, LogFName  );
    }
    
    //Constructor
    public VRObj26( IObserverMsg observer, String LogFName ) {
        this.observerHl= observer;    	
        this.LogFName  = LogFName;
        toApplMsg = "msg(vrinfo, event, VRObj26, none, CONTENT, 0)";
    }
    
    public void connect(String virtualRobotIp) {
    	try {
    		String dockerEnv = CommUtils.getEnvvarValue("VIRTUAL_ENV");
	    	if(  dockerEnv != null) { //In docker ...
	    		CommUtils.outmagenta("VRObj26 | connect VIRTUAL_ENV="+ dockerEnv);
	    		CommUtils.delay(4000); //Gve time to start the gui
	    		//conn = WsConnection.create("robotoutgui25:8085","wsupdates");
	    		wsconn    = new WsconnObserver(dockerEnv,this); 
	    		conn      = wsconn.getConn();  
	    	}else {
	    		CommUtils.outmagenta("VRObj26 | connect virtualRobotIp="+  virtualRobotIp);
		        wsconn    = new WsconnObserver(virtualRobotIp,this); 
		        conn      = wsconn.getConn();  
	    	}
    	}catch( Exception e) {
    		CommUtils.outred("VRObj26 | error:"+e.getMessage());
    	}
    }   
    public Interaction getConn() {
        return conn;
    }
    public void setTrace(boolean v){
        tracing = v;
        wsconn.setTrace(v);
    } 
    @Override
    public void turnLeft() throws Exception {
        requestSynch(VrobotMsgs.turnleftcmd);
    }

    @Override
    public void turnRight() throws Exception {
        requestSynch(VrobotMsgs.turnrightcmd);
    }

    @Override
    public void forward(int time) throws Exception {
        String forwardMsg = VrobotMsgs.forwardcmd.replace("TIME", "" + time);
        if( tracing ) CommUtils.outgreen("VRObj26 | forwardMsg= " + forwardMsg);
        startTimer();
        conn.forward( forwardMsg );
    }

    @Override
    public void backward(int time) throws Exception {
        startTimer();
        conn.forward(VrobotMsgs.backwardcmd.replace("TIME", "" + time));
    }

    @Override
    public void halt()   {
    	try {
	        conn.forward(VrobotMsgs.haltcmd);
	        CommUtils.delay(1); //wait for halt completion since halt on ws does not send answer
		}catch(Exception e) {
			CommUtils.outred("halt error (strange....)" + e.getMessage() );
		}
    }
   
    protected void handleSonar(JSONObject jsonObj) {
        if (jsonObj.get("sonarName") != null) { //defensive
         	if( tracing ) 
         		CommUtils.outred("VRObj26 | handleSonar " + jsonObj);
             long d = (long) jsonObj.get("distance") ;
             if( d < 0 ) d = -d;
     		//CommUtils.outred("VRObj26 | handleSonarrrrrrrrrrr d=" + d);
            IApplMessage sonarEvent = CommUtils.buildEvent( "VRObj26","sonardata","sonar(" + d + ")");
            emitInfo(sonarEvent);
          }
    }
    
    protected void handleMoveok(String move) {
    	elapsed = getDuration();
        if( tracing )              
        	CommUtils.outcyan("VRObj26 | handleMoveok:" + move + " elapsed=" + elapsed );               
       if( ( move.equals("turnLeft") || move.equals("turnRight")) ){
            activateWaiting( move,"true" );
            return;
        } 
       if( ! doingStepSynch ) {    
            String wenvInfo = toApplMsg.replace("CONTENT", "vrinfo("+move+"," +elapsed +")");
            IApplMessage msg = new ApplMessage(wenvInfo);
            emitInfo(msg);   
       }else {  //move is a forwardcmd for step synch
           String wenvInfo = toApplMsg.replace("CONTENT", "vrinfo(step,"+elapsed+")");
           IApplMessage msg = new ApplMessage(wenvInfo);
           emitInfo(msg);   
           activateWaiting(move,"true" );
       }        
    }
    
    protected void handleMoveko(String move) {
     	//int elapsed = getDuration();  //elapsed Già valutata da collision
        if( tracing )              
        	CommUtils.outblack("VRObj26 | handleMoveKO:" + move + " elapsed=" + elapsed + " doingStepSynch " + doingStepSynch);               
    	if (move.contains("collision") || move.contains("interrupted") ) {
            if(  ! doingStepSynch ) {   
            	 move = move.split("-")[0];   //xxx-collsion
                 String wenvInfo = "msg(vrinfo, event, VRObj26, none, CONTENT, 0)"
                         .replace("CONTENT","vrinfo(" + move + "_ko ,"+ elapsed + ")");
                 IApplMessage msg = new ApplMessage(wenvInfo);   
                 if( tracing )  
                	 CommUtils.outred("VRObj26 | handleMoveKO msg:" + msg);
                 emitInfo(msg);
            } else {
            	String m = "stepFail("+elapsed+")";
                IApplMessage stepFailEvent = CommUtils.buildEvent("VRObj26","stepFail",m );
                //CommUtils.outred("VRObj26 | handleMoveKO msg:" + stepFailEvent);
                doingStepSynch = false;
                emitInfo(stepFailEvent);
            }
            activateWaiting(move,"false"  );
        }    	
    } 
    
    protected void handleCollision( String cause) {
    	elapsed = getDuration();
    	halt(); //interrompe la move che provocato la collision
        //CommUtils.outred( "VRObj26 | handleCollision:"   );               
        IApplMessage collisionEvent = CommUtils.buildEvent(
                "VRObj26","vrinfo","vrinfo(obstacle_XXX,collision)".replace("XXX", cause) );
        //CommUtils.outyellow("VRObj26 | emit " + collisionEvent + " elapsed=" + elapsed);
        emitInfo(collisionEvent);
    }
    

    /*
     * --------------------------------------------
     * Timer part
     * --------------------------------------------
     */
    private Long timeStart = 0L;

    public void startTimer() {
        elapsed = 0;
        timeStart = System.currentTimeMillis();
    }

    public int getDuration() {
        long duration = (System.currentTimeMillis() - timeStart);
        return (int) duration;
    }

/*
 * --------------------------------------------
 * The synch Step moves
 * --------------------------------------------
 */

    @Override
    public boolean step(long time) throws Exception {
        doingStepSynch = true;
        String cmd    = VrobotMsgs.forwardcmd.replace("TIME", "" + time);
        String result = requestSynch(cmd);
        doingStepSynch = false; 
        return result.contains("true");
    }

    protected String requestSynch(String msg) throws Exception {
        asynchMoveResult = null;
        //Invio fire-and.forget e attendo modifica di  moveResult da update
        if( tracing ) CommUtils.outyellow("VRObj26 | requestSynch " + msg);
        startTimer();
        conn.forward(msg);
        String result = waitForResult();
        if( tracing ) CommUtils.outyellow("VRObj26 | requestSynch result=" + result);
        return result;  
    }
    
    protected String waitForResult() throws Exception {
        synchronized (this) {
            while (asynchMoveResult == null) {
                wait();
            }
            return asynchMoveResult;
        }
    }
    protected void activateWaiting(String move, String endmove){
        if( tracing ) CommUtils.outmagenta("VRObj26 | activateWaiting ... " + endmove);
        synchronized (this) {  //sblocca request sincrona per checkRobotAtHome
            asynchMoveResult = endmove;
            notifyAll();
        }
    }

    /*
     * --------------------------------------------
     * The 'geyser' effect method
     * --------------------------------------------
     */
    protected void emitInfo(IApplMessage info) {
    	logger.append(info.toString());
    	if( observerHl != null) {
      		//String payload = info.msgContent();
      		observerHl.update( info );
    	}else {
    		if( tracing ) CommUtils.outmagenta("VRObj26  | emitInfo " + info );
     	}
     }
    
    /*
     * A main just to test ...
     */
    public static void main(String[] args) throws Exception {
        CommUtils.aboutThreads("Before start - ");
        VRObj26 appl = VRObj26.create( null,null );
        appl.connect("localhost");
        CommUtils.aboutThreads("At end - ");
    }

}

/*
 * --------------------------------------------------------
 * Osserva la connessione Ws e chiama opportune
 * funzioni del VRObj26
 * --------------------------------------------------------
 */
class WsconnObserver implements IObserver{
	protected VRObj26 robot;
	protected String vitualRobotIp;
	protected Interaction conn;
    protected boolean tracing         = false;
    protected LogUtils logger         = new LogUtils("wsobs");


	public WsconnObserver(String vitualRobotIp, VRObj26 robot) {
	   	this.vitualRobotIp = vitualRobotIp;
		this.robot         = robot;
		connect(vitualRobotIp);
	}
	
    protected void connect(String vitualRobotIp) {
       this.conn = 
        	ConnectionFactory.createClientSupport(ProtocolType.ws,vitualRobotIp+":8091","");
        //SET itself as ath observer over the WSconnection
   		//CommUtils.outred("WsconnObserverrrrrrrrrrrrrrrrrrrrrrrrrrrrrrr " + conn);
        ((WsConnection) conn).addObserver(  this );
        //CommUtils.outgreen("WsconnObserver | CREATED in " + Thread.currentThread().getName());
    }

    public void setTrace(boolean v){
        tracing = v;
    }
    public Interaction getConn() {
        return conn;
    }


	@Override
	public void update(Observable o, Object arg) {
		update( arg.toString() );	
	}

	@Override
	public void update(String info) {
        try {            
            JSONObject jsonObj = CommUtils.parseForJson(info);
            
            if( tracing )              
            CommUtils.outgreen(
                "     wsconn | update:"  
                        + " jsonObj=" + jsonObj //+ " doingStep=" + doingStepSynch
                        + " " + Thread.currentThread().getName());    //Grizzly            
            logger.append(info);
            if (jsonObj == null) {
            	CommUtils.outred("     wsconn | update ERROR Json:" + info);
                return;
            }            
            if (jsonObj.get("endmove") != null) {      
            	//int delta          = getDuration();
            	String move        = jsonObj.get("move").toString();
            	//CommUtils.outred("     wsconn | endmove " + move + " delta=" + delta);
               boolean moveresult = checkMoveResult(jsonObj);
                if (moveresult) {
                	robot.handleMoveok(  move );
                    return;
                } 
                 else {
                	robot.handleMoveko(  move );
                	 return;
                }
              
            }//endmove!=null
            if (jsonObj.get("collision") != null) {
                String cause = jsonObj.get("target").toString();
                robot.handleCollision(cause);
                return;
            }          	 
            if (info.contains("_notallowed")) {
                CommUtils.outred("     wsconn | update WARNING!!! _notallowed unexpected in " + info);
                robot.halt();
                return;
            }
            if (jsonObj.get("sonarName") != null) {
            	//if( sonarDataNum++ == 0 )
            	robot.handleSonar(jsonObj);  //potrebbe entrare in loop 
                return;
            } 
        } catch (Exception e) {
            CommUtils.outred("     wsconn | update ERROR:" + e.getMessage());
        }
		
	}//update
	
	   protected boolean checkMoveResult(JSONObject jsonObj) {
	    	//CommUtils.outyellow("checkMoveResult: " + jsonObj);
	        boolean moveresult= jsonObj.get("endmove").toString().contains("true");
	        return moveresult;   	
	    }

}//WsconnObserver
