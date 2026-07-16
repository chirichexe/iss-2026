package it.unibo.kactor

import unibo.basicomm23.mqtt.MqttConnection
import unibo.basicomm23.utils.CommUtils
import org.eclipse.paho.client.mqttv3.MqttMessage


class MqttUtils(val owner: String ) {
	protected var eventId: String? = "mqttConn"
	protected var eventMsg: String? = ""
	protected var mqtttraceOn = false
	protected lateinit var workActor: ActorBasic
	protected var isConnected = false

	protected val RETAIN = false;

	public val mqttConn = MqttConnection(owner)

	fun trace(msg: String) {
		if (mqtttraceOn) println("$msg")
	}

	fun connect(clientid: String, brokerAddr: String): Boolean {
		sysUtil.traceprintln("               %%% MqttUtils connect $clientid to $brokerAddr")
		return mqttConn.connect(clientid, brokerAddr)
	}

	fun connectDone(): Boolean {
		return mqttConn.isConnected
	}

	fun disconnect() {
		try {
			sysUtil.traceprintln("		%%% MqttUtils $owner | disconnect  $mqttConn")
			if( mqttConn.isConnected()   ) mqttConn.disconnect()
		} catch (e: Exception) {
			println("		%%% MqttUtils $owner | disconnect ERROR ${e}")
		}
	}


	fun subscribe(actor: ActorBasic, topic: String) {
		sysUtil.traceprintln("               %%% MqttUtils ${actor.name} subscribe to topic=$topic  "  )
		try {
			this.workActor = actor //actor implements MqttCallback
			//client.setCallback(this)
			//mqttConn.setCallback(actor)
			mqttConn.subscribe(topic,actor)   //JAN25
		} catch (e: Exception) {
			CommUtils.outred("               %%% MqttUtils $owner | ${actor.name} subscribe topic=$topic ERROR=$e ")
		}
	}

	fun publish(topic: String, msg: String, qos: Int = 2, retain: Boolean = false) {
		sysUtil.traceprintln("               %%% MqttUtils publish $msg on $topic qos=$qos retain=$retain") //mqttConn=$mqttConn
 		try{
			mqttConn.publish(topic, msg, qos, retain)
		}catch( e : Exception){
 			CommUtils.outred(" %%% MqttUtils publish ERROR: perhaps no MQTT"   );
		}
	}

	//Dec2023
	fun cleartopic(topic: String){
		mqttConn.cleartopic(topic)
	}
}



