package unibo.basicomm23.mqtt;

import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.utils.ColorsOut;
import unibo.basicomm23.utils.CommUtilsOrig;

public class MqttDemoEssential {
private String demotopicOut  = "uniboDemoOut";
private String demotopicIn   = "uniboDemoIn";
private String brokerAddr    = "tcp://localhost:1883"; //"tcp://broker.hivemq.com"; // : 1883  OPTIONAL
//"tcp://test.mosquitto.org"


private final String caller    = "demo";
private final String requestId = "query";

//String sender, String msgId, String payload, String dest
private String helloMsg  = CommUtilsOrig.buildDispatch(caller, "cmd",    "hello",    "someone").toString();
//private String aRequest  = Utils.buildRequest(caller,  requestId,"getTime",  receiver).toString();
//private String aReply    = Utils.buildReply(receiver,  requestId, "ANSWER",   caller).toString();

 
//private MqttConnection mqtt;


public MqttDemoEssential() {
	init();
}

protected void init() {
//	mqtt = MqttConnection.create(brokerAddr, caller+"-"+demotopicIn+"-"+demotopicOut);
//	
//	MqttAnswerHandler h = new MqttAnswerHandler("demoH", null, mqtt.getQueue() );
//	mqtt.subscribe(demotopicOut, h);
}

/*
public void simulateReceiver(String name) {
	new Thread() {
		public void run() {
		try {
 			CommUtils.outgreen( name + " as receiver STARTS  "  );
			String inputMNsg = mqtt.receiveMsg();
			CommUtils.outgreen( name + " as receiver RECEIVED:"+ inputMNsg );
 		} catch (Exception e) {
 			CommUtils.outred(name + " as receiver  | Error:" + e.getMessage());
	 	}
		}//run
	}.start();
}



protected void end() {
	ColorsOut.outappl( " | disconnect ", ColorsOut.CYAN);
	//mqtt.unsubscribe( topic );	
	mqtt.disconnect();
}

public void doSendReceive() {
	simulateReceiver("r1");
	//SENDER part
	try {
		mqtt.forward("helloMsg");	//No risposta
		mqtt.forward(helloMsg);	    //receiver RECEIVED:msg(cmd,dispatch,demo,someone,hello,0)
		CommUtils.delay(1000);
		end();  //Se no si ha poi un  connectionLost  
		ColorsOut.outappl("doSendReceive BYE BYE" , ColorsOut.BLUE);
 	} catch (Exception e) {
		e.printStackTrace();
	}
}
*/
/*
 * -------------------------------------------------
 * REQUEST - RESPONSE
 * -------------------------------------------------
 */


public void simulateCalled( String name ) {
	new Thread() {
		public void run() {
		try {
 			CommUtilsOrig.outgreen(name + " | STARTS "  );
 			MqttConnection mqtt = MqttConnection.create(brokerAddr, name+"-demoOut-demoIn");
			String inputMNsg = mqtt.receiveMsg();  //Si blocca sulla coda popolata da 
			CommUtilsOrig.outgreen(name + " | RECEIVED:"+ inputMNsg );
//Elaborate and send answer			
 			IApplMessage msgInput = new ApplMessage(inputMNsg);
 			CommUtilsOrig.outgreen(name + " | input=" + msgInput + " topic="+demotopicOut );
			String caller = msgInput.msgSender();
			String reqId  = msgInput.msgId();
			String myReply = CommUtilsOrig.buildReply(name ,  reqId, "ANSWER", caller  ).toString();
			String content = "time('" + java.time.LocalTime.now() + "')";
 			
			String answer  = myReply.replace("ANSWER", content );  
			CommUtilsOrig.outgreen( name + " | replies:"+ answer );
			mqtt.reply(answer);  			
 		} catch (Exception e) {
 			CommUtilsOrig.outred(name + " | Error:" + e.getMessage());
	 	}
		}//run 
	}.start(); 
}

public void doRequestRespond() {
	simulateCalled( "called1");
//	simulateCalled( "called2");
	//Caller part 	
	try {
		CommUtilsOrig.outgreen(caller + " | STARTS "  );
		MqttConnection mqtt = MqttConnection.create(brokerAddr, caller+"-demoIn-demoOut" );
		String req1    = CommUtilsOrig.buildRequest(caller,  requestId,"getTime",  "called1").toString();
		String answer1 = mqtt.request(req1);	 //blocking
		ColorsOut.outappl(caller + " RECEIVED answer1:"+ answer1, ColorsOut.BLACK);
//		String req2 = Utils.buildRequest(caller,  requestId,"getTime",  "called2").toString();
//		String answer2 = mqtt.request(req2);	   //blocking
//		Colors.outappl(caller + " RECEIVED answer2:"+ answer2, Colors.BLACK);
 	} catch (Exception e) {
		e.printStackTrace();
	}
}
 
	public static void main(String[] args) throws Exception  {
		//RadarSystemConfig.mqttBrokerAddr  = "tcp://localhost:1883";  
		//RadarSystemConfig.mqttBrokerAddr  = "tcp://broker.hivemq.com";
		 //mqttBrokerAddr  = "tcp://test.mosquitto.org";
		//RadarSystemConfig.mqttBrokerAddr  = "tcp://mqtt.eclipse.org:1883";  //NO
		MqttDemoEssential sys = new MqttDemoEssential();	
		
 		//sys.doSendReceive();
  		sys.doRequestRespond();
  		
  		System.exit(0);
 	}

}
