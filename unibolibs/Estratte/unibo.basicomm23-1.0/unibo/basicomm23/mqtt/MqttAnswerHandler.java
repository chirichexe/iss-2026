package unibo.basicomm23.mqtt;

import java.util.concurrent.BlockingQueue;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import unibo.basicomm23.interfaces.*;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.utils.ColorsOut;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.CommUtilsOrig;

 
public class MqttAnswerHandler  implements IApplMsgHandlerMqtt{
private String name;
private  IApplMsgHandler handler ;
private BlockingQueue<String> blockingQueue = null;
private MqttConnection conn;

	public MqttAnswerHandler( String name, MqttConnection conn, IApplMsgHandler handler, BlockingQueue<String> blockingQueue ) {
		this.name=name;
		this.conn = conn;
		//CommUtils.outred("CREATE MqttAnswerHandler:" + name );
		this.handler       = handler;
 		this.blockingQueue = blockingQueue;
 		//Colors.out(name+" | MqttAnswerHandler CREATED blockingQueue=" + blockingQueue, Colors.ANSI_PURPLE);
	}

//	public MqttAnswerHandler( BlockingQueue<String> blockingQueue ) {
//		this("mqttAnswHandler"+ n++, blockingQueue);
//	}

	@Override
	public void messageArrived(String topic, MqttMessage message)   {
//  		CommUtils.outcyan(name + " MqttAnswerHandler | messageArrived:" + message + " on topic="+topic  );
// 		Colors.out(name + " | msgId=" +
// 				message.getId() + "  Qos="+ message.getQos() + " isDuplicate="
// 				+ message.isDuplicate() + " payload=" + message.getPayload().length,
// 				Colors.ANSI_PURPLE );
 		if( message.getPayload().length == 1 ) {
 			elaborate("sorry", conn ); //MqttConnection.getSupport()
 			return;  //perch� RICEVO 0 ???
 		}
		try { //Perhaps we receive a structured message
			IApplMessage msgInput = new ApplMessage(message.toString());
			elaborate(msgInput, conn ); //MqttConnection.getSupport()
		}catch( Exception e) {
			CommUtilsOrig.outred(name +  " (WARNING) MqttAnswerHandler | messageArrived non IApplMessage:"+ message  );
			//elaborate(message.toString(), MqttConnection.getSupport() );  //JAN25
 		}
	}
 		@Override
		public void elaborate(IApplMessage message, Interaction conn) {
			try {
				blockingQueue.put(message.toString());
//				CommUtils.outcyan(name +  " MqttAnswerHandler | elaborate (put) IApplMessage:" + message  );
			} catch (Exception e) {
				ColorsOut.outerr(name +  " MqttAnswerHandler | elaborate IApplMessage ERROR " + e.getMessage());
			}
		}


		public void elaborate(String message, Interaction conn) {
			try {
				blockingQueue.put(message.toString());
				ColorsOut.outappl(name +  " MqttAnswerHandler | elaborate (put) String: " + message, ColorsOut.ANSI_YELLOW);
			} catch (Exception e) {
				ColorsOut.outerr(" MqttAnswerHandler | elaborate ERROR " + e.getMessage());
			}

		}
		@Override
		public void deliveryComplete(IMqttDeliveryToken token){
			ColorsOut.out(name +  " MqttAnswerHandler | deliveryComplete token=" + token.getMessageId(), ColorsOut.ANSI_YELLOW);
		}

		@Override
		public void connectionLost(Throwable cause) {
			ColorsOut.outerr(name +  " MqttAnswerHandler | connectionLost cause="+cause);
			
		}

		@Override
		public String getName() {
			// TODO Auto-generated method stub
			return null;
		}

}
