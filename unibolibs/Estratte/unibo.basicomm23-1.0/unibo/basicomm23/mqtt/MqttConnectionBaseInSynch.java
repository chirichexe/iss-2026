package unibo.basicomm23.mqtt;

import unibo.basicomm23.mqtt.MqttConnectionCallbackForReceive;
import unibo.basicomm23.mqtt.MqttSupport;
import unibo.basicomm23.utils.CommUtilsOrig;

/*
 * Connessione che permette solo ricezione di informazione cone reeive bloccante
 */
public class MqttConnectionBaseInSynch   {
	protected String topic;
	protected String clientid;
	protected MqttConnectionCallbackForReceive forreceivhandler;
	protected MqttSupport mqttSupport;	
	
	public MqttConnectionBaseInSynch(String mqttBrokerAddr, String clientid, String topic ) {
		mqttSupport = new MqttSupport();
		mqttSupport.connectToBroker(clientid,mqttBrokerAddr);       
        this.topic          = topic;
        this.clientid       = clientid;
        forreceivhandler = new MqttConnectionCallbackForReceive( clientid+"_CB");
        mqttSupport.subscribe(topic, forreceivhandler);
//        CommUtils.outyellow("MqttConnectionBaseInSynch CREATED for " + clientid + " on "+ topic);
    }
	
 
	public String receive() throws Exception{
//		CommUtils.outyellow("MqttConnectionBaseInSynch receive for " + clientid + " on "+ topic);
		String s = forreceivhandler.receive();
 		return s;
	}

	public void disconnect() {
		mqttSupport.disconnect();
	}

}
