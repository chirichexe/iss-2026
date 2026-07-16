package unibo.basicomm23.mqtt;

import java.util.concurrent.BlockingQueue;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import unibo.basicomm23.interfaces.*;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.utils.ColorsOut;
import unibo.basicomm23.utils.CommUtilsOrig;

 
public class MqttInteractionAnswerHandler  implements MqttCallback{
private String name;
private  IApplMsgHandler handler ;
private BlockingQueue<String> blockingQueue = null;
private MqttInteraction conn;

	public MqttInteractionAnswerHandler( String name, MqttInteraction conn,  BlockingQueue<String> blockingQueue ) {
		this.name=name;
		this.conn = conn;
		CommUtilsOrig.outred("CREATE MqttInteractionAnswerHandler:" + name );
  		this.blockingQueue = blockingQueue;
 		//Colors.out(name+" | MqttInteractionAnswerHandler CREATED blockingQueue=" + blockingQueue, Colors.ANSI_PURPLE);
	}


	@Override
	public void messageArrived(String topic, MqttMessage message)   {
 		CommUtilsOrig.outmagenta(name + " MqttInteractionAnswerHandler | messageArrived:" + message + " on topic="+topic  );
// 		Colors.out(name + " | msgId=" +
// 				message.getId() + "  Qos="+ message.getQos() + " isDuplicate="
// 				+ message.isDuplicate() + " payload=" + message.getPayload().length,
// 				Colors.ANSI_PURPLE );
 		if( message.getPayload().length == 1 ) {
 			elaborate("sorry", conn );  
 			return;  //perch� RICEVO 0 ???
 		}
		try { //Perhaps we receive a structured message
			IApplMessage msgInput = new ApplMessage(message.toString());
			elaborate(msgInput, conn ); //MqttConnection.getSupport()
		}catch( Exception e) {
			CommUtilsOrig.outred(name +  " (WARNING) MqttInteractionAnswerHandler | messageArrived non IApplMessage:"+ message  );
			//elaborate(message.toString(), MqttConnection.getSupport() );  //JAN25
 		}
	}
 		//@Override
		public void elaborate(IApplMessage message, MqttInteraction conn) {
			try {
				blockingQueue.put(message.toString());
				CommUtilsOrig.outcyan(name +  " MqttInteractionAnswerHandler | elaborate (put) IApplMessage:" + message  );
			} catch (Exception e) {
				ColorsOut.outerr(name +  " MqttInteractionAnswerHandler | elaborate IApplMessage ERROR " + e.getMessage());
			}
		}


		public void elaborate(String message, MqttInteraction conn) {
			try {
				blockingQueue.put(message.toString());
				ColorsOut.outappl(name +  " MqttInteractionAnswerHandler | elaborate (put) String: " + message, ColorsOut.ANSI_YELLOW);
			} catch (Exception e) {
				ColorsOut.outerr(" MqttInteractionAnswerHandler | elaborate ERROR " + e.getMessage());
			}

		}

/*
 * 
 */
		
		@Override
		public void deliveryComplete(IMqttDeliveryToken token){
			ColorsOut.out(name +  " MqttInteractionAnswerHandler | deliveryComplete token=" + token.getMessageId(), ColorsOut.ANSI_YELLOW);
		}

		@Override
		public void connectionLost(Throwable cause) {
			ColorsOut.outerr(name +  " MqttInteractionAnswerHandler | connectionLost cause="+cause);
			
		}

 

}
