package unibo.basicomm23.mqtt;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import unibo.basicomm23.utils.ColorsOut;
import unibo.basicomm23.utils.CommUtilsOrig;
 

public class MqttConnectionCallbackForReceive implements MqttCallback{
 	private BlockingQueue<String> blockingQueue = null;
 	private String name;

 	public MqttConnectionCallbackForReceive( String name  ) {
 		this.name = name;
// 		CommUtils.outgreen(name + " | CREATED a MqttConnectionCallbackForReceive");
 		blockingQueue = new LinkedBlockingDeque<String>(10);
  	}
 	public MqttConnectionCallbackForReceive( String name, BlockingQueue<String> blockingQueue  ) {
 		this.name = name;
 		//CommUtils.outgreen(name + " | CREATED ");
 		this.blockingQueue = blockingQueue;
  	}

		@Override
		public void connectionLost(Throwable cause) {
			ColorsOut.outerr(name + " | connectionLost cause="+cause);
	 	}

		@Override
		public void messageArrived(String topic, MqttMessage message)   {
			try {
//				CommUtils.outyellow( name + " |  messageArrived:" + message + " queue=" + blockingQueue.size());
				if( blockingQueue != null ) {
					blockingQueue.put( message.toString() );	
//					CommUtils.outyellow(name + " | inserted N=" + blockingQueue.size() + " " + message );
				}
			} catch (Exception e) {
				ColorsOut.outerr(name + " | messageArrived Error:"+e.getMessage());		
			}
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			try {
//				CommUtils.outyellow(name + " | deliveryComplete token=" 
//			       + token.getMessage() + " client=" + token.getClient().getClientId() );
			} catch (Exception e) {
				CommUtilsOrig.outred(name + " | deliveryComplete Error:"+e.getMessage());		
			}
	 	}
		
		public String receive()   {
			try {
//				CommUtils.outyellow(name + " | receiving N=" + blockingQueue.size() );
				String mm =  blockingQueue.take();
//				CommUtils.outyellow(name + " | receive N=" + blockingQueue.size() + " taken:" + mm );
				return mm;
			} catch (InterruptedException e) {
				return null;
			}
		}
		
}
