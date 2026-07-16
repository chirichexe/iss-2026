package unibo.basicomm23.mqtt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.IApplMsgHandler;
import unibo.basicomm23.interfaces.IApplMsgHandlerMqtt;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.utils.ColorsOut;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.CommUtilsOrig;
import unibo.basicomm23.utils.Connection;
 


/*
 * MqttConnection implementa Interaction e quindi realizza il concetto di connessione nel caso di MQTT.
 * USATA dal supporto qak
 */
public class MqttConnection extends Connection {  
 	
protected  MqttClient client;
protected BlockingQueue<String> blockingQueue = new LinkedBlockingDeque<String>(10);
protected String clientid;

protected String brokerAddr;
protected String topicOutput = "";
protected String topicInput = "";

protected boolean isConnected = false; 

	public static synchronized MqttConnection create(   ) {
		return new MqttConnection();
	}
	//DEC2024
	public static synchronized MqttConnection create(
			String clientName, String mqttBrokerAddr, IApplMsgHandlerMqtt obj ) throws Exception {
		MqttConnection mqttConn = new MqttConnection(   ); //MqttConnection.create();
		mqttConn.connectMqttWithHandler(mqttBrokerAddr, obj, clientName);		
		return  mqttConn;		 
	}
	//JULY 2025
	public static synchronized MqttConnection create(
			String clientName, String mqttBrokerAddr, String singleentry, IApplMsgHandlerMqtt obj ) throws Exception {
		try {
			MqttConnection mqttConn = new MqttConnection( clientName, mqttBrokerAddr);
			//mqttConn.connectMqttWithHandler(mqttBrokerAddr, obj, clientName);	
			mqttConn.subscribe(singleentry);       //per ricevere richieste ed eventi
			mqttConn.setTopic( singleentry );	   //per inviare comandi
			mqttConn.getClient().setCallback(obj);
			CommUtilsOrig.outmagenta("MqttConnection create with handler "  );
			return  mqttConn;		 
		}catch( Exception e) {
			CommUtilsOrig.outred("MqttConnection create ERROR " + e.getMessage());
			return null;
		}
	}
	public static synchronized MqttConnection create(
			String clientName, String mqttBrokerAddr, String singleentry) throws Exception {
		MqttConnection mqttConn = new MqttConnection(  clientName, mqttBrokerAddr, singleentry ); 
		mqttConn.subscribe(singleentry);
		return  mqttConn;		 
	}
	public static synchronized MqttConnection create( String mqttBrokerAddr, String entry ) {
		//if( mqttSup == null  ) {
			//CommUtils.outcyan(" CREATE " + entry);
			String[] parts = entry.split("-");
			String clientName = parts[0];
			String topicIn    = parts[1];
			String topicOut   = parts[2];
			MqttConnection mqttSup = new MqttConnection(clientName, mqttBrokerAddr, topicIn, topicOut);
//		}
		return mqttSup;
	}
	
	public void setTopic(String topic) {
		topicOutput = topic;
	}
	
	public MqttConnection(    ) {
		
	}

    public MqttConnection( String owner  ) {
    	clientid    = owner;
    	//Mantento per compaticnilità con unibo.qak - vedi MqqtUtils di ActorBasic
    }
 
	public MqttConnection( String clientName, String mqttBrokerAddr ) {
		clientid    = clientName;
		connectToBroker(clientName, mqttBrokerAddr);	
	}
	public MqttConnection(String clientName, String mqttBrokerAddr, String topic) { //, String topicToSubscribe
    	setTopic(topic);
    	clientid    = clientName;
    	connectToBroker(clientName, mqttBrokerAddr);	
    }
    //DEC2024
    public MqttConnection(String clientName, String mqttBrokerAddr, String topicIn, String topicOut) {  
    	CommUtilsOrig.outyellow("			MqttConnection NEW " + clientName + " in=" + topicIn + " out=" + topicOut);
    	clientid    = clientName;
    	topicOutput = topicOut;
    	topicInput  = topicIn;
    	//setTopic(topicOut);
    	connectToBroker(clientid, mqttBrokerAddr);	
    	subscribe(clientid, topicIn);
    }
    
    public MqttClient getClient() {
    	return client;
    }
     
    public BlockingQueue<String> getQueue() {
    	return blockingQueue;
    }
     
    //Mantento per compaticnilità con unibo.qak
    public boolean connect(String clientid,  String brokerAddr) {
    	connectToBroker(clientid,brokerAddr);
    	return true;
    }
    
    public boolean isConnected(){
    	return isConnected;
    }

    
	public MqttClient connectMqttWithHandler(String brokerAddr, IApplMsgHandlerMqtt obj, String name ) throws MqttException { 
			client = new MqttClient(brokerAddr , name); 
			CommUtilsOrig.outyellow("			MqttConnection  connectMqttWithHandler client=" + client);
			client.setCallback(obj);  //IMPORTANT DEC2024
			client.connect();
			this.clientid   = client.getClientId();
			return client;
	}

 
    
    public boolean connectToBroker(String clientid,  String brokerAddr) {
    	if( isConnected ) return true;
		try {
			this.brokerAddr = brokerAddr;
			MemoryPersistence persistence = new MemoryPersistence();
			//persistence: per evitare see https://github.com/eclipse/paho.mqtt.java/issues/794
			//CommUtils.outyellow("MqttConnection | connectToBroker clientid=" + clientid + " brokerAddr="+brokerAddr );
			client          = new MqttClient(brokerAddr, clientid, persistence);
			MqttConnectOptions connOpts = new MqttConnectOptions();
			
		    connOpts.setCleanSession(true);
		    //connOpts.setUserName(userName);
		    //connOpts.setPassword(passWord.toCharArray());
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

			//CommUtils.outyellow("MqttConnection | doing client.connect" + client.getClientId()  );

//			MqttConnectionCallback cb = new MqttConnectionCallback(blockingQueue);
//			client.setCallback( cb );  //IMPORTANT

			
			client.connect(connOpts);      //Blocking
			this.clientid   = clientid;  
			isConnected     = true;
			if( trace )
				CommUtilsOrig.outmagenta("			MqttConnection  | connected client " + client.getClientId() + " to broker " + brokerAddr );
			return true;
		} catch (MqttException e) {
			CommUtilsOrig.outred("			MqttConnection | connect Error:" + e.getMessage());
			return false;
		}    	
    }
 
   //Introduced for unibo.qakactor22
    public void setCallback( MqttCallback h ) {
    	client.setCallback(h);
    }
    //Introduced for unibo.qakactor22
    public void subscribe( String t )   {
     	   //	if( trace ) 
     	   		CommUtilsOrig.outmagenta(clientid + " | MqttConnection subscribe TO:" + t);
      		MqttAnswerHandler h = new MqttAnswerHandler(clientid+"H", this, null, blockingQueue);
      		subscribe(t, h);

    }
    public void subscribe( String t,MqttCallback handler )   {
     	   	if( trace ) 
     	   		CommUtilsOrig.outmagenta(clientid + " | MqttConnection subscribe with callback " + t);
     	    subscribe( clientid, t, handler);
     }

 
	public void disconnect() {
		try {
			client.disconnect();
			client.close();
			isConnected = false;
			CommUtilsOrig.outmagenta(clientid + " | disconnect " );
		} catch (MqttException e) {
			ColorsOut.outerr("MqttConnection  | disconnect Error:" + e.getMessage());
		}
	}
	
	
	public void unsubscribe( String topic ) {
		try {
			client.unsubscribe(topic);
			ColorsOut.out("unsubscribed " + clientid + " topic=" + topic + " blockingQueue=" + blockingQueue, ColorsOut.CYAN);
		} catch (MqttException e) {
			ColorsOut.outerr("MqttConnection  | unsubscribe Error:" + e.getMessage());
		}
	}
	

	//To receive and handle a message (command or request)
	public void subscribe ( String topic, IApplMsgHandlerMqtt handler) {
		CommUtilsOrig.outred("MqttConnection  | subscribe with handler " + clientid + " to " +  topic   );
		subscribe(clientid, topic, handler);
	}

	//TO BE REMOVED .... since clientid is not used NO JAN25
	public void subscribe(String clientid, String topic, MqttCallback callback) {
		//CommUtils.outred("subscribe callback " + clientid + " to " +  topic   );
		try {
			client.setCallback( callback );
			client.subscribe( topic );
		} catch (MqttException e) {
			CommUtilsOrig.outred("MqttConnection  | subscribe Error:" + e.getMessage());
		}
	}
	
	//To receive and handle an answer
	public void subscribe(String clientid, String answertopic) {
		CommUtilsOrig.outyellow("		MqttConnection  | subscribe " + clientid + " to " + answertopic   );
		//subscribe( clientid, answertopic, new MqttConnectionCallback(client.getClientId() , blockingQueue));
		//JAN24
		subscribe( clientid, answertopic, new MqttConnectionCallback(clientid, blockingQueue));
	}

	public void publish(String topic, String msg ) {
		publish( topic, msg, 2, false );
	}
	
	//JAN25
	public void send(  String msg ) {
		publish( topicOutput, msg, 2, false );
	}	
	public void publish(String topic, String msg, int qos, boolean retain) {
		//CommUtils.outyellow("MqttConnection  | publish " + msg + " on " + topic );
		MqttMessage message = new MqttMessage();
		if (qos == 0 || qos == 1 || qos == 2) {
			//qos=0 fire and forget; qos=1 at least once(default);qos=2 exactly once
			message.setQos(qos);
		}
		try {
			//CommUtils.outyellow("MqttConnection  | publish topic=" + topic + " msg=" + msg  );
			message.setPayload(msg.toString().getBytes());		 
			client.publish(topic, message);
			//ColorsOut.outappl("MqttConnection  | publish-DONE on topic=" + topic, ColorsOut.CYAN );
		} catch (MqttException e) {
			CommUtilsOrig.outred("		MqttConnection  | publish Error "  + client.getClientId() + " " + e.getMessage());
		}
	}
	
//----------------------------------------------------	
	//ASSUNZIONE: forward viene usato per inviare dispatch
	@Override
	public void forward(String msg) throws Exception {
		//ColorsOut.outappl("MqttConnection  | forward " + msg , ColorsOut.BLUE);
		try{
			new ApplMessage(msg); //no exception => we can publish
		}catch( Exception e ) { //The message is not structured
			CommUtilsOrig.outmagenta("MqttConnection  | forward WARNING: no ApplMessage"  );
//			IApplMessage msgAppl = CommUtils.buildDispatch("mqtt", "cmd", msg, "unknown");
//			msg = msgAppl.toString();  //DEC2024
		}				
		publish(topicOutput, msg, 2, false);	
	}


	
	public MqttClient setupConnectionForAnswer(String answerTopicName) {
		try{
			MemoryPersistence persistence = new MemoryPersistence();
			//persistence: per evitare see https://github.com/eclipse/paho.mqtt.java/issues/794
			//CommUtils.outmagenta("MqttConnection | setupConnectionForAnswer answerTopicName=" + answerTopicName);
			
			MqttClient clientAnswer    = new MqttClient(brokerAddr, "clientAnswer", persistence);
			MqttConnectOptions options = new MqttConnectOptions();
			options.setKeepAliveInterval(480);
			options.setWill("unibo/clienterrors", "crashed".getBytes(), 2, true);  
			clientAnswer.connect(options);
	 		//Colors.out("MqttConnection | connected clientAnswer to " + brokerAddr  , Colors.CYAN);
			IApplMsgHandler handler = null;  //TODO
	 		MqttAnswerHandler ah    = new MqttAnswerHandler( "replyH", this, handler, blockingQueue );	
	 		clientAnswer.setCallback(ah);
	 		clientAnswer.subscribe(answerTopicName);		
	 		return clientAnswer;
		}catch( Exception e ) { //The message is not structured
			CommUtilsOrig.outred("MqttConnection | setupConnectionForAnswer ERROR:" + e.getMessage());
			return null;
		}
	}
	
	@Override
	public String request(String msg) throws Exception { //msg should contain the name of the sender
//		CommUtils.outblue("... request " + msg + " by clientid=" + clientid  );
		//INVIO RICHIESTA su topic
		IApplMessage requestMsg;
		try{
			 requestMsg = new ApplMessage(msg); //no exception => we can publish
		}catch( Exception e ) { //The message is not structured
			ColorsOut.outerr("MqttConnection | request warning:" + e.getMessage());
			requestMsg = CommUtilsOrig.buildRequest("mqtt", "request", msg, "unknown");
		}			
		
//Preparo per ricevere la risposta
		String sender   = requestMsg.msgSender();
		String reqid    = requestMsg.msgId();
		String receiver = requestMsg.msgReceiver();
		String answerTopicName = "answ_"+reqid+"_"+sender;
		//String answerTopicName = "answ_"+reqid+"_"+receiver; //JAN24
 		//CommUtils.outblue("MqttConnection request answerTopicName="+answerTopicName );
		MqttClient clientAnswer = setupConnectionForAnswer(answerTopicName);

//Invio la richiesta 		
		publish(topicOutput, requestMsg.toString(), 2, false);	
 		
 		//ATTESA RISPOSTA su answerTopic. See MqttConnectionCallback
		String answer = receiveMsg();
		clientAnswer.disconnect();
		clientAnswer.close();
		return answer;
		
 	}
	
	protected String waitFroAnswerPolling(MqttClient clientAnswer ) {
		String answer = null;
		while( answer== null ) {
			answer=blockingQueue.poll() ;
			if( trace ) ColorsOut.out("MqttConnection | blockingQueue-poll answer=" + answer, ColorsOut.CYAN  );
			CommUtilsOrig.delay(200); //il client ApplMsgHandler dovrebbe andare ...
		}	
		ColorsOut.out("MqttConnection | request-answer=" + answer + " blockingQueue=" + blockingQueue, ColorsOut.CYAN);
 		try {
 			ApplMessage msgAnswer = new ApplMessage(answer); //answer is structured
 			answer = msgAnswer.msgContent(); 		
 			//Disconnect ancd close the answer client
 			clientAnswer.disconnect();
 			clientAnswer.close();
   		}catch(Exception e) {
 			ColorsOut.outerr("MqttConnection | request-answer ERROR: " + e.getMessage()   ); 			
 		}
		return answer;
	}
	
	protected String waitFroAnswerBlocking(MqttClient clientAnswer ) {
		String answer = null;
		try {
			answer =   receiveMsg();
//			answer=blockingQueue.take() ;
//			ColorsOut.out("MqttConnection | request-answer=" + answer + " blockingQueue=" + blockingQueue, ColorsOut.CYAN);
//		 	ApplMessage msgAnswer = new ApplMessage(answer); //answer is structured
//		 	answer = msgAnswer.msgContent(); 		
		 	//Disconnect ancd close the answer client
		 	clientAnswer.disconnect();
		 	clientAnswer.close();
		}catch(Exception e) {
		 	ColorsOut.outerr("MqttConnection | request-answer ERROR: " + e.getMessage()   ); 			
		}
		return answer;
	}

	@Override
	public String receiveMsg()   {
 		//CommUtils.outyellow("MqttConnection | receiveMsg ... blockingQueue=" + blockingQueue.size()  );
		String answer = "data unknown ...";
 		try {
			answer = blockingQueue.take();
			if( trace ) ColorsOut.outappl("		MqttConnection | receiveMsg answer="+answer + " blockingQueue.size=" + blockingQueue.size() , ColorsOut.CYAN);
			IApplMessage msgAnswer = new ApplMessage(answer); //answer is structured
			answer = msgAnswer.toString();
			if( trace ) 
				ColorsOut.outappl("		MqttConnection | receiveMsg answer=" + answer , ColorsOut.CYAN);
		}catch( Exception e){
			//answer = e.getMessage().toString();
			CommUtilsOrig.outyellow("		MqttConnection | received a non  IApplMessage"  );
		}
		return answer;
	}
	
	public void reply(String msg) throws Exception {
		try {
			//CommUtils.outred("MqttConnection | reply msg="+msg );
			ApplMessage m = new ApplMessage(msg);
			//TODO: Si potrebbe tenere traccia della richiesta e del caller
			String dest  = m.msgReceiver();
			String reqid = m.msgId();
			String answerTopicName = "answ_"+reqid+"_"+dest;
			//CommUtils.outred("MqttConnection | reply  answerTopicName="+answerTopicName );
			publish(answerTopicName,msg,2,false);  
			CommUtils.outblue("MqttConnection | reply on " + answerTopicName );
 		}catch(Exception e) {
			ColorsOut.outerr("		MqttConnection | reply msg not structured " + msg);
			//publish(topic+"Answer",msg,0,false);
		}
	}
	
//	protected String receiveMsg(String topic, BlockingQueue<String> bq) throws Exception{
//		ColorsOut.out("MqttConnection | receiveMsg2 topic=" + topic + " blockingQueue=" + bq, ColorsOut.CYAN);
//  		String answer = bq.take();
//		ColorsOut.out("MqttConnection | receiveMsg2 answer=" + answer + " blockingQueue=" + bq, ColorsOut.CYAN);
// 		try {
// 			ApplMessage msg = new ApplMessage(answer); //answer is structured
// 			answer = msg.msgContent(); 			
// 		}catch(Exception e) {
// 			ColorsOut.outerr("MqttConnection | receiveMsg2 " + answer + " not structured"   ); 
//  		}
//		client.unsubscribe(topic);
//		return answer;		 
//	}
	
//	protected String receiveMsg(String topic) throws Exception{
//		ColorsOut.out("MqttConnection | receiveMsg topic=" + topic + " blockingQueue=" + blockingQueue, ColorsOut.CYAN);
//		//subscribe("MqttConnection",topic);
// 		String answer = blockingQueue.take();
//		//Colors.out("MqttConnection | receiveMsg answer=" + answer + " blockingQueue=" + blockingQueue, Colors.CYAN);
// 		try {
// 			ApplMessage msg = new ApplMessage(answer); //answer is structured
// 			answer = msg.msgContent(); 			
// 		}catch(Exception e) {
// 			ColorsOut.outerr("MqttConnection | receiveMsg " + answer + " not structured"   ); 			
// 		}
//		client.unsubscribe(topic);
//		return answer;		 
//	}




	@Override
	public void close()   {
		try {
			client.disconnect();
			client.close();
			ColorsOut.outappl("		MqttConnection | client disconnected and closed ", ColorsOut.CYAN);
		} catch (MqttException e) {
			ColorsOut.outerr("		MqttConnection  | close Error:" + e.getMessage());
 		}
	}

 	//Dec2023
	public void cleartopic(String topic){
		try {
			String msg    = (new byte[0]).toString();
			CommUtilsOrig.outblue("		MqttConnection  | cleartopic m=" + msg);
			publish(topic, msg,0,true);
		} catch ( Exception e) {
			CommUtilsOrig.outred("		MqttConnection  | cleartopic Error:" + e.getMessage());
		}
	}

	public void settrace(boolean b) {
		trace = b;
	}

}
