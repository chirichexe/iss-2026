package unibo.basicomm23.mqtt;
import unibo.basicomm23.mqtt.MqttSupport;
import unibo.basicomm23.utils.CommUtilsOrig;

/*
 * Connessione che permette solo trasmissione di informazione
 */
public class MqttConnectionBaseOut  {
	protected String topic;
	protected MqttSupport mqttSupport;
 	
	public MqttConnectionBaseOut(String mqttBrokerAddr, String clientid, String topic ) {
		mqttSupport = new MqttSupport();
		mqttSupport.connectToBroker(clientid, mqttBrokerAddr);
//        this.brokerAddr     = mqttBrokerAddr;
//        this.clientid       = clientid;
        this.topic          = topic;
//        connectToBroker(clientid, mqttBrokerAddr);
       }
	
//	@Override
//	public void subscribe ( String topic, MqttCallback handler) {
//		CommUtils.outred("MqttConnectionBaseOut | subscribe not allowed"  );
//	}
	
 	public void send(String msg) throws Exception{
		//CommUtils.outyellow(clientid + " MqttConnectionBase | send " + msg + " on:" + topic);
 		mqttSupport.publish(topic, msg);
	}

 	public void disconnect() {
 		mqttSupport.disconnect();
 	}
 
}
