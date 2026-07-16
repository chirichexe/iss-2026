package unibo.basicomm23.mqtt;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import unibo.basicomm23.mqtt.MqttSupport;
import unibo.basicomm23.utils.CommUtilsOrig;

/*
 * Connessione che permette solo ricezione di informazione con callback
 */
public class MqttConnectionBaseInAsynch  {
	protected String topic;
	protected MqttSupport mqttSupport;
 	
	public MqttConnectionBaseInAsynch(String mqttBrokerAddr, String clientid, String topic, MqttCallback callback) {
//		CommUtils.outblack("MqttConnectionBaseInAsynch  mqttBrokerAddr=" + mqttBrokerAddr);
		mqttSupport = new MqttSupport();
		mqttSupport.connectToBroker(clientid,mqttBrokerAddr);
		this.topic          = topic;
        if( callback != null ) mqttSupport.subscribe(topic, callback);
     }
	
   
}
