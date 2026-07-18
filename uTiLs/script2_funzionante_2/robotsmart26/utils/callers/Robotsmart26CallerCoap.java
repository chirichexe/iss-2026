package callers;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import it.unibo.kactor.sysUtil;
import unibo.basicomm23.coap.CoapConnection;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;

/**
 * Robotsmart26CallerCoap - COAP Client for Basic Robot Communication
 * 
 * 
 * @author Unibo Robotsmart26 Team
 * @version 2026
 */
public class Robotsmart26CallerCoap {
	/** Logger for this class */
	private static final Logger logger = LoggerFactory.getLogger("Robotsmart26Caller");
 
 	
    /** Client name identifier */
    protected String name;
    
    /** Communication channel for COAP interactions */
    protected Interaction commChannel;
    
    /** Protocol type (COAP) */
    protected ProtocolType protocol;
    
    /** Host address for robot service */
    protected String hostAddr;
    
    /** Entry point for robot service */
    protected String entry;
    
    /** Connection status flag */
    protected boolean connected = false;
    
    /** COAP observe relation for monitoring robot state */
    protected CoapObserveRelation relationObsRobotsmart;
    protected CoapObserveRelation relationObsRobotmnemo;
    protected CoapObserveRelation relationObsPlanexec;
    
    protected Interaction connToPlanexec;
  
	/**
	 * Constructor initializes the COAP caller and clears log files.
	 * Sets up logging and prepares the client for robot communication.
	 */
	public Robotsmart26CallerCoap() {
		sysUtil.clearlog("./logs/robotsmart26.log");
		logger.info("avviato correttamente.");		
	}

	/**
	 * Establishes a COAP connection to the robot service.
	 * Creates a client connection using the ConnectionFactory with COAP protocol.
	 * 
	 * @param host The hostname or IP address of the robot service
	 * @param entry The entry point for the robot service
	 * @return An Interaction object for communication, or null if connection fails
	 */
	protected Interaction connectToService(String host, String entry) {
		try {			 
  				CommUtils.outcyan("connectService Hostname: " + host);
				CommUtils.outcyan("connectService Port:     " + entry);
 				Interaction connSupport = 
 						ConnectionFactory.createClientSupport23(ProtocolType.coap, host, entry);
 				logger.info("connected");
 				return connSupport;
 		} catch (Exception e) {
			CommUtils.outred("ERROR:" + e.getMessage());
			return null;
		}
	}
 
  	/**
  	 * Sets up observation of the basicrobot actor via COAP.
  	 * Creates an observer to monitor robot state changes and sonar data.
  	 * Detects sonar alarms and provides audio feedback for obstacles.
  	 */
	protected void observerobotsmart(Interaction conn) {
  		//Interaction conn = ConnectionFactory.createClientSupport23(ProtocolType.coap,"localhost:8020", "ctxrobotsmart/robotsmart");
		CoapClient client = ((CoapConnection)conn).getClient();
	    CommUtils.outblue("callerCoap Interaction conn");
		relationObsRobotsmart = client.observe(
				new CoapHandler() {
					@Override public void onLoad(CoapResponse response) {
						String content = response.getResponseText();
						CommUtils.outmagenta("robotsmart observer | " + content);
						if( content.contains("sonar")) {
							CommUtils.outred(content);
							java.awt.Toolkit.getDefaultToolkit().beep();  // Audio alarm for sonar detection
						}
//						else CommUtils.outgreen(content);
					}					
					@Override public void onError() {
						CommUtils.outred("basicrobot OBSERVING FAILED");
					}
				});	
	}
	
	/**
	 * Sets up observation of the robotpos actor via COAP.
	 * Creates an observer to monitor robot position and state changes.
	 * Provides real-time updates on robot position and direction.
	 */
	protected void observeRobotmnemo( ) {
  			Interaction conn = ConnectionFactory.createClientSupport23(ProtocolType.coap,"localhost:8020", "ctxrobotsmart/robotmnemo");
  			if( conn == null ) return;
			CoapClient client = ((CoapConnection)conn).getClient();
		    CommUtils.outgreen("observeRobotmnemo");
			relationObsRobotmnemo = client.observe(
					new CoapHandler() {
						@Override public void onLoad(CoapResponse response) {
							String content = response.getResponseText();
							CommUtils.outgreen("observeRobotmnemo | " + content);
 						}					
						@Override public void onError() {
							CommUtils.outred("robotpos OBSERVING FAILED");
						}
					});	
	}
	
	public void observePlanexec() {
		connToPlanexec = ConnectionFactory.createClientSupport23(ProtocolType.coap,"localhost:8020", "ctxrobotsmart/planexec");
		if( connToPlanexec == null ) return;
//		CoapClient client = ((CoapConnection)connToPlanexec).getClient();
	    CommUtils.outmagenta("observePlanexecccccc");
		relationObsPlanexec = ((CoapConnection) connToPlanexec).observeResource( //client.observe(
				new CoapHandler() {
					@Override public void onLoad(CoapResponse response) {
						String content = response.getResponseText();
						CommUtils.outmagenta("observePlanexec | " + content);
						}					
					@Override public void onError() {
						CommUtils.outred("observePlanexec OBSERVING FAILED");
					}
				});	
	}

 
	
	public void doSomeCmd() throws Exception   {
 		observePlanexec();
//		observeRobotmnemo(); 
		
		Interaction conn = connectToService("localhost:8020", "ctxrobotsmart/robotsmart");
		if( conn != null ) {	
			CommUtils.outcyan("doSomeCmd");
			
			conn.forward(Robotsmart26Cmds.setHome.toString());
			conn.forward(Robotsmart26Cmds.setHome.toString());
			// Send basic movement commands
			conn.forward(Robotsmart26Cmds.cmdl.toString());  // Turn left
			CommUtils.delay(1000);
//			conn.forward(Robotsmart26Cmds.cmdr.toString());  // Turn right
//			CommUtils.delay(1000); 
			
			String answer = conn.request(Robotsmart26Cmds.doplan.toString().replace("PLAN", "wwwwww"));  // 
			CommUtils.outcyan("doSomeCmd doplan answer=" + answer);
			
			if( answer.contains("doplanfailed")) {
				CommUtils.outred("some move todo ...");
				terminateObservations();
			}
			CommUtils.delay(1000);

			answer = conn.request(Robotsmart26Cmds.doplan.toString().replace("PLAN", "llwwwwwwl"));  // 
			CommUtils.outcyan("doSomeCmd doplan answer=" + answer);
			if( answer.contains("doplanfailed")) {
				CommUtils.outred("some move todo at the and ...");
			}
//			CommUtils.delay(1000);

			terminateObservations();
		}
	} 
	
	public void terminateObservations() {
		CommUtils.outmagenta("doSomeCmd terminateObservations ");
		if (relationObsRobotsmart != null) {
			/*
			 * Invia un messaggio CoAP al server (solitamente un messaggio di RST o un pacchetto di deregistrazione). 
			 * Il server viene informato immediatamente che non deve più inviare notifiche. 
			 * Questo libera risorse sia sul client che sul server (e risparmia banda sul network Docker/WiFi).
			 * 
			 * ricorda di inviare un ultimo messaggio di "cleanup" al tuo attore prima di cancellare la relazione, 
			 * così che il sistema sappia che quel flusso di dati è ufficialmente terminato.
			 * 
			 * 
			 */
		    relationObsRobotsmart.proactiveCancel();
		}			
		if (relationObsRobotmnemo != null) {
			relationObsRobotmnemo.proactiveCancel();
		}
		if (relationObsPlanexec != null) {
			relationObsPlanexec.proactiveCancel();
			CoapClient client = ((CoapConnection)connToPlanexec).getClient();
			CommUtils.delay(4000); //Dai tempo alla rete di smaltire l'invio.
			CommUtils.outblue("terminateObservationsPlanexec BYE ");
			client.shutdown();
			
		}
		//Ci può essere persistenza dei messaggi nella rete o nei buffer di sistema.
		//il "residuo" potrebbe essere nello stato del bridge di Docker.
		CommUtils.delay(2000); //Dai tempo alla rete di smaltire l'invio.
		CommUtils.outmagenta("terminateObservations BYE ");
        System.exit(0);
		
	}

	/**
	 * Main entry point for the COAP caller application.
	 * Creates an instance and executes robot commands via COAP.
	 * 
	 * @param args Command line arguments (not used)
	 * @throws Exception if any operation fails
	 */
	public static void main(String[] args) throws Exception{
		Robotsmart26CallerCoap caller = new Robotsmart26CallerCoap();
		//caller.doSomeCmd();
		caller.observePlanexec();
		CommUtils.delay(60000); //Dai tempo alla rete di smaltire l'invio.
		caller.terminateObservations();
		CommUtils.outmagenta("main BYE ");
	}
}
