package unibo.basicomm23.mqtt;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.IApplMsgHandlerMqtt;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.utils.CommUtilsOrig;

public abstract class AbstractMqttConnCallback implements IApplMsgHandlerMqtt{
	protected String name;
	
	public AbstractMqttConnCallback(String name) {
		this.name=name;
	}

	@Override
	public void connectionLost(Throwable cause) {
		 CommUtilsOrig.outred("AbstractMqttConnectionCallback | connectionLost cause="+cause);	
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception{
		//CommUtils.outcyan("AbstractMqttConnectionCallback | messageArrived:" + message);
		try {
			IApplMessage msgInput = new ApplMessage(message.toString());
			elaborate(msgInput, null);
		} catch (Exception e) {
			elabArrivedNessage(topic, message );
		}
		
	}
	
	protected abstract void elabArrivedNessage(String topic, MqttMessage message);

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		//CommUtils.outred("AbstractMqttConnectionCallback | deliveryComplete  " );	 		
	}

	@Override
	public String getName() {
 		return name;
	}

	@Override
	public void elaborate(IApplMessage message, Interaction conn) {
		CommUtilsOrig.outred("AbstractMqttConnectionCallback | elaborate not implemenetd  " );	
	}

}
