package unibo.basicomm23.mqtt;

import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import unibo.basicomm23.utils.ColorsOut;
import unibo.basicomm23.utils.CommUtilsOrig;

/*
 * Just a support
 */
public class MqttSupport  {  
 	
protected MqttClient client;
protected String clientid;
protected String brokerAddr;
 


/*
 * --------------------------------------------
 * Factory methods
  * --------------------------------------------
  */
	public static synchronized MqttSupport create(   ) {
		return new MqttSupport();
	}

/*
 * --------------------------------------------
 * Constructors
 * --------------------------------------------
 */
	
	public MqttSupport(    ) {
		
	}
/*

/*
 * --------------------------------------------
 * Connection
 * --------------------------------------------
 */    
    
    public boolean connectToBroker(String clientid,  String brokerAddr) {
 		try {
 			//this.clientid   = clientid;
			this.brokerAddr = brokerAddr;
			MemoryPersistence persistence = new MemoryPersistence();
			//persistence: per evitare see https://github.com/eclipse/paho.mqtt.java/issues/794
			//CommUtils.outyellow("MqttSupport | connectToBroker clientid=" + clientid + " brokerAddr="+brokerAddr );
			client          = new MqttClient(brokerAddr, clientid, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			
		    connOpts.setCleanSession(true);
		    /* 
		     * This value, measured in seconds, defines the maximum time interval the client  
 			 *  will wait for the network connection to the MQTT server to be established
		    */
		    connOpts.setConnectionTimeout(60); 
		    /* 
		     * This value, measured in seconds, defines the maximum time interval between 
		     * messages sent or received
		     */
		    connOpts.setKeepAliveInterval(30); 
		    connOpts.setAutomaticReconnect(true);					
//			options.setKeepAliveInterval(480);
//			options.setWill("unibo/clienterrors", "crashed".getBytes(), 2, true);
			//CommUtils.outyellow("MqttSupport | doing client.connect" + client.getClientId()  );			
			client.connect(connOpts);      //Blocking
			this.clientid   = clientid;  
			//CommUtils.outyellow("MqttSupport | connected client " + client.getClientId() + " to broker " + brokerAddr );
			return true;
		} catch (MqttException e) {
			CommUtilsOrig.outred("MqttSupport  | connect Error:" + e.getMessage());
			return false;
		}    	
    }
 
  
	public void disconnect() {
		try {
			client.disconnect(); //Blocking
			client.close();
			//CommUtils.outcyan(clientid + " MqttSupport | disconnected and closed " );
		} catch (MqttException e) {
			ColorsOut.outerr("MqttSupport  | disconnect Error:" + e.getMessage());
		}
	}

 	//Dec2023
	public void cleartopic(String topic){
		try {
			String msg    = (new byte[0]).toString();
			CommUtilsOrig.outyellow("MqttSupport  | cleartopic m=" + msg);
			publish(topic, msg,0,true);
		} catch ( Exception e) {
			CommUtilsOrig.outred("MqttSupport  | cleartopic Error:" + e.getMessage());
		}
	}
	

	
/*
 * --------------------------------------------
 * Subscribe
 * --------------------------------------------
 */

	public void subscribe ( String topic, MqttCallback handler) {
		//CommUtils.outyellow("MqttSupport | subscribe with MqttCallback " + handler + " to " +  topic );
		try {
			client.setCallback( handler );
			client.subscribe( topic );
		} catch (MqttException e) {
			ColorsOut.outerr("MqttSupport | subscribe Error:" + e.getMessage());
		}
	}

	public void subscribe (String topic, IMqttMessageListener messageListener) {
		try {			 
			client.subscribe( topic, messageListener);
		} catch (MqttException e) {
			ColorsOut.outerr("MqttSupport | subscribe Error:" + e.getMessage());
		}
	}
	
 
	
	public void unsubscribe( String topic ) {
		try {
			client.unsubscribe(topic);
			CommUtilsOrig.outcyan("unsubscribed " + clientid + " topic=" + topic  );
		} catch (MqttException e) {
			ColorsOut.outerr("MqttSupport  | unsubscribe Error:" + e.getMessage());
		}
	}
/*
 * --------------------------------------------
 * Publish
 * --------------------------------------------
 */
	
	public void publish(String topic, String msg ) {
		publish( topic, msg, 2, false );
	}
	
	public void publish(String topic, String msg, int qos, boolean retain) {
		//CommUtils.outyellow("MqttSupport  | publish " + msg + " on " + topic );
		MqttMessage message = new MqttMessage();
		if (qos == 0 || qos == 1 || qos == 2) {
			//qos=0 fire and forget; qos=1 at least once(default);qos=2 exactly once
			message.setQos(qos);
		}
		if (retain) {
			message.setRetained(true);
		}
		try {
			//CommUtils.outyellow("MqttSupport  | publish topic=" + topic + " msg=" + msg  );
			message.setPayload(msg.toString().getBytes());		 
			client.publish(topic, message);
		} catch (MqttException e) {
			CommUtilsOrig.outred("MqttSupport  | publish Error "  + client.getClientId() + " " + e.getMessage());
		}
	}


}//MqttSupport
