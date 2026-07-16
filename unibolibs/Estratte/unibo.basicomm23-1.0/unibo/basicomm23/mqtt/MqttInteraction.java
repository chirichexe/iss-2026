package unibo.basicomm23.mqtt;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.utils.CommUtilsOrig;

/*
 * MqttInteraction 
 */
public class MqttInteraction implements Interaction{
	protected String mqttBrokerAddr;
	protected String topicOutput = "";
	protected String topicInput = "";
	protected MqttConnectionBaseOut mqttconnOut;
	protected MqttConnectionBaseInSynch mqttconnIn;
	private String clientid;
	private String answerTopicName;


    public static MqttInteraction create (String mqttBrokerAddr, String entry){ 
 		String[] parts = entry.split("-");
		String clientName = parts[0];
		String topicIn    = parts[1];
		String topicOut   = parts[2];
		return new MqttInteraction(clientName, mqttBrokerAddr, topicIn, topicOut);
    }

    public MqttInteraction(
    		String clientName, String mqttBrokerAddr, String topicIn, String topicOut){ 
    	this.mqttBrokerAddr = mqttBrokerAddr;

    	clientid    = clientName;
    	topicOutput = topicOut;
    	topicInput  = topicIn;
    	
//     	CommUtils.outblack("MqttInteraction | "+
//     	 clientName+" topicIn=" + topicIn " topicOut=" + topicOut);
    	mqttconnOut = new MqttConnectionBaseOut( mqttBrokerAddr,  clientid+"_out",  topicOut ); 
//    	CommUtils.outblack("MqttInteraction | "+clientName+" Synch on Topic:" + topicIn);
    	mqttconnIn  = new MqttConnectionBaseInSynch( mqttBrokerAddr,  clientid+"_in",  topicIn ); 
    }

    /*
     * -------------------------------------------------------
     * Interaction2023
     * -------------------------------------------------------
    */
	@Override
	public void forward(IApplMessage msg) throws Exception {
		//CommUtils.outgreen(clientid + " MqttInteraction | forward " + msg  );
		forward(msg.toString());
	}

	/*
	 * Più sender possono usare la stessa connessione verso un receiver
	 * Ogni sender riceve ogni mesg emesso dal receiver
	 * Un sender che fa una request deve ricevere la SUA rispsota, che non deve essere visibile agli altri sender
	 */
	@Override
	public IApplMessage request(IApplMessage requestMsg) throws Exception {
		String sender   = requestMsg.msgSender();
		String reqid    = requestMsg.msgId();
		String receiver = requestMsg.msgReceiver();
		answerTopicName = "answ_"+reqid+"_"+sender;
		//CommUtils.outcyan("MqttInteraction | request answerTopicName="+answerTopicName );
		
		//PREPARO LA CONNESSIONE PER LA RISPOSTA
		//CommUtils.outcyan(clientid + " MqttInteraction | create " + clientid+reqid + " for receive on:" + answerTopicName); 
		MqttConnectionBaseInSynch answerconn = 
				new MqttConnectionBaseInSynch( mqttBrokerAddr, clientid+reqid, answerTopicName );
        
		//Invio la richiesta  
		forward(requestMsg.toString());
		//ATTENDO LA RISPOSTA		
		String answer = answerconn.receive();
		//CommUtils.outcyan("MqttInteraction | request answer="+answer  );
		answerconn.disconnect();
		return new ApplMessage(answer);
	}

	public IApplMessage requestNaive(IApplMessage requestMsg) throws Exception {
		String answer = request( requestMsg.toString() );
		return new ApplMessage(answer);
	}
	
	@Override
	public IApplMessage request(IApplMessage msg, int tout) throws Exception {
		return null;
	}


	@Override
	public void reply(IApplMessage msg) throws Exception {
		String dest  = msg.msgReceiver();
		String reqid = msg.msgId();
		String answerTopicName = "answ_"+reqid+"_"+dest;
//		CommUtils.outcyan("MqttInteraction | reply answerTopicName=" + answerTopicName); 
		//mqttconn.publish(answerTopicName, msg.toString());
		MqttSupport support = new MqttSupport();
		support.connectToBroker( clientid,   mqttBrokerAddr);
		support.publish( answerTopicName,msg.toString() );
		support.disconnect();
//		CommUtils.outcyan("MqttInteraction | reply DONE:" + msg); 		
	}

	 
	public void replyNaive(IApplMessage msg) throws Exception {
		reply(msg.toString());
	}
	
	@Override
	public IApplMessage receive() throws Exception {
//		CommUtils.outred(clientid + " MqttInteraction | receiving ... on " + topicInput ); 	
		String msg =  receiveMsg();
		if( msg == null ) return null;
		try {
		    IApplMessage msgAnswer = new ApplMessage(msg); //answer is structured
            msg = msgAnswer.toString();
            return  msgAnswer;
		} catch (Exception e) {
			CommUtilsOrig.outyellow("MqttInteraction | WARINIG: received a non IApplMessage");
			return null;
		}
	}
 
    /*
     * -------------------------------------------------------
     * Interaction2021
     * -------------------------------------------------------
    */
	@Override
	public void forward(String msg) throws Exception {
		mqttconnOut.send( msg );		
	}

	@Override
	public String request(String msg) throws Exception {
		//Invio la richiesta  
		mqttconnOut.send( msg );
		//ATTENDO LA RISPOSTA		
// 		CommUtils.outcyan("MqttInteraction | waits on "+ topicInput  );
		String answer = mqttconnIn.receive();
//		CommUtils.outcyan("MqttInteraction | request answer="+answer  );
		return answer;
   }

 
	@Override
	public void reply(String msg) throws Exception {
		mqttconnOut.send(msg.toString());
	}
	
	@Override
	public String receiveMsg() throws Exception {
//		CommUtils.outyellow("MqttInteraction | receiveMsg  "    );
		String answer = "data unknown ...";
		answer = mqttconnIn.receive();
 		return answer;
	}	
	
	
	@Override
	public void close() throws Exception {
		CommUtilsOrig.outyellow("MqttInteraction | CLOSING .. "    );
		mqttconnOut.disconnect();	
		mqttconnIn.disconnect();
	}
	
	@Override
	public String toString() {
		return "MqttInteraction client=" + clientid + " topicIn=" + topicInput + " topicOut=" + topicOutput;
	}
 
}
