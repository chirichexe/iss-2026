package callers;

import java.nio.charset.StandardCharsets;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.IApplMsgHandlerMqtt;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.mqtt.MqttConnection;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;
import unibo.basicomm23.msg.ApplMessage;

/**
 * Robotsmart26CallerMqtt - MQTT Client for Basic Robot Communication
 * 
 * 
 * @author Unibo Robotsmart26 Team
 * @version 2026
 */
public class Robotsmart26CallerMqtt implements IApplMsgHandlerMqtt {
	
	/** MQTT broker URL - localhost for local development */
	private final String MqttBroker = "tcp://192.168.0.218:1883";//"tcp://broker.hivemq.com"; //
	
	/** MQTT topic for robot communication */
	private String appltopicIn = "robotsmart26in";
    
    /** MQTT connection interface */
    private Interaction conn;
    
  
    /** Robot commands handler for accessing predefined robot operations */
    private Robotsmart26Cmds robot = new Robotsmart26Cmds();

	/**
	 * Establishes connection to the MQTT service.
	 * Creates an MQTT connection and subscribes to the robot topic.
	 * The connection automatically receives all messages sent to the topic,
	 * including messages sent by this client itself.
	 * 
	 * @return Interaction object representing the MQTT connection, or null if connection fails
	 */
	protected Interaction connectToService(String MqttBroker, String topic) {
		try {			  
			CommUtils.outblue("connectToService ......... " + MqttBroker);
			MqttConnection mqttConn = MqttConnection.create(getName(), MqttBroker, topic, this);
  		      	// MqttConnection usa appltopicIn per publish
  		      	//e fa subscribe(appltopicIn+"_out",this)
  		      	// plus information (events) from wenv
			mqttConn.cleartopic(topic);
			return mqttConn;
 		} catch (Exception e) {
			CommUtils.outred("ERROR:" + e.getMessage());
			return null;
		}
	}

	/**
	 * Waits for events for a specified duration.
	 * Useful for receiving and processing robot events and responses.
	 * 
	 * @param time Duration to wait in milliseconds
	 * @throws Exception if waiting fails
	 */
	public void waitForEvents(int time) throws Exception   {
		CommUtils.outblue("waitForEvents " + time);
		Thread.sleep(time);
	}

	/**
	 * Sends a sequence of basic movement commands to the robot via MQTT.
	 * Demonstrates how to use the MQTT connection to control the robot
	 * with simple movement commands.
	 * 
	 * @throws Exception if communication fails
	 */
	public void doSomeCmd() throws Exception   {
		conn = connectToService("tcp://192.168.0.218:1883", "robotsmart26in");
		if( conn != null ) {
			CommUtils.outcyan("connectService doSomeCmd ...");
			//setup
			conn.forward(Robotsmart26Cmds.setHome.toString());
//			conn.forward(Robotsmart26Cmds.restoreplanbuildelay.toString());
			// Send basic movement commands
			conn.forward(Robotsmart26Cmds.cmdl.toString());  // Turn left
			CommUtils.delay(1000);
			conn.forward(Robotsmart26Cmds.cmdr.toString());  // Turn right
			CommUtils.delay(1000); 
			 
			CommUtils.outcyan("connectService doSomePlan");
			String answer = conn.request(Robotsmart26Cmds.doplan.toString().replace("PLAN", "lwwwwww"));  // 
			CommUtils.outcyan("doSomeCmd doplan answer=" + answer);
			CommUtils.delay(1000);

			answer = conn.request(Robotsmart26Cmds.doplan.toString().replace("PLAN", "llwwwwwwl"));  // 
			CommUtils.outcyan("doSomeCmd doplan answer=" + answer);
			CommUtils.delay(1000);

			CommUtils.outcyan("doSomeCmd BYE");
		}
        System.exit(0);
	}



	/**
	 * Returns the name identifier for this MQTT client.
	 * Used by the MQTT connection for client identification.
	 * 
	 * @return The client name
	 */
	@Override
	public String getName() {
		return "mqttCaller";
	}

	/**
	 * Handles application messages received via MQTT.
	 * Processes robot responses and events, with special handling for alarm events.
	 * 
	 * @param message The received application message
	 * @param conn The connection that received the message
	 */
	@Override
	public void elaborate(IApplMessage message, Interaction conn) {
		//CommUtils.outyellow("mqttCaller elaborate " + message);
		if( message.isEvent() && message.msgId().contains("alarm")   ) {
			CommUtils.outred("ALARM: " + message.msgContent());
		}
	}

	/**
	 * Handles MQTT connection loss events.
	 * Logs the connection loss and can be extended for reconnection logic.
	 * 
	 * @param cause The cause of the connection loss
	 */
	@Override
	public void connectionLost(Throwable cause) {
		CommUtils.outred("mqttCaller connectionLost " + cause.getMessage());
	}

	/**
	 * Handles raw MQTT messages received on subscribed topics.
	 * Converts MQTT messages to application messages and processes them.
	 * 
	 * @param topic The MQTT topic that received the message
	 * @param message The raw MQTT message
	 * @throws Exception if message processing fails
	 */
	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {
		String content = new String(message.getPayload(), StandardCharsets.UTF_8);  
		CommUtils.outyellow("mqttCaller messageArrived .... " + content);
		if( content.startsWith("[")) return;  //EMISSION SPORCA
//		for(byte b : message.getPayload()) {
//		    System.out.print(b + " ");
//		}
		IApplMessage applMessage = new ApplMessage(content);
		elaborate(applMessage, conn); 
	}
  
	/**
	 * Handles MQTT message delivery completion.
	 * Called when a message has been successfully delivered to the broker.
	 * 
	 * @param token The delivery token for the completed message
	 */
	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		CommUtils.outblue("mqttCaller deliveryComplete");
	}

	/**
	 * Main entry point for the MQTT caller application.
	 * Creates an instance and executes robot commands via MQTT.
	 * 
	 * Available options:
	 * - doSomeCmd(): Basic movement commands
	 * - doJob(): Predefined path execution (tf25)
	 * 
	 * @param args Command line arguments (not used)
	 * @throws Exception if any operation fails
	 */
	public static void main(String[] args) throws Exception  {
 		Robotsmart26CallerMqtt caller = new Robotsmart26CallerMqtt();
		caller.doSomeCmd();  //  basic movement demo
		 
	}
}
