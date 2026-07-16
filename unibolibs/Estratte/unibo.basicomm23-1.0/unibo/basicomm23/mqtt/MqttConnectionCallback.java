package unibo.basicomm23.mqtt;

import java.util.concurrent.BlockingQueue;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import unibo.basicomm23.utils.ColorsOut;
import unibo.basicomm23.utils.CommUtilsOrig;
 
 


public class MqttConnectionCallback implements MqttCallback{
 	private BlockingQueue<String> blockingQueue = null;

 	public MqttConnectionCallback( BlockingQueue<String> blockingQueue ) {
 		this.blockingQueue = blockingQueue;
		//CommUtils.outblue("MqttConnectionCallback |  blockingQueue=" + blockingQueue);
	}

		public MqttConnectionCallback(String clientName, BlockingQueue<String> blockingQueue ) {
			//CommUtils.outblue("MqttConnectionCallback | clientName=" + clientName + " blockingQueue=" + blockingQueue);
			this.blockingQueue = blockingQueue;
		}
		@Override
		public void connectionLost(Throwable cause) {
			ColorsOut.outerr("MqttConnectionCallback | connectionLost cause="+cause);
	 	}

		@Override
		public void messageArrived(String topic, MqttMessage message) throws Exception {
			//CommUtils.outmagenta("MqttConnectionCallback | messageArrived:" + message  );
			if( blockingQueue != null ) blockingQueue.put( message.toString() );	
		}

		@Override
		public void deliveryComplete(IMqttDeliveryToken token) {
			try {
//				Colors.outappl("MqttConnectionCallback | deliveryComplete token=" 
//			       + token.getMessage() + " client=" + token.getClient().getClientId() , Colors.ANSI_YELLOW);
			} catch (Exception e) {
				ColorsOut.outerr("MqttConnectionCallback | deliveryComplete Error:"+e.getMessage());		
			}
	 	}
		
}
