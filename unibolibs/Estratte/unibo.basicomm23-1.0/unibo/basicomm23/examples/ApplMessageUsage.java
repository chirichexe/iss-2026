package unibo.basicomm23.examples;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.msg.ApplMessage;

public class ApplMessageUsage {
	
	public void doJob() {
		// Creazione di un messaggio di applicazione
		IApplMessage msg = new ApplMessage("eval", "request", "client", "server", "hello(server)", "0");
		System.out.println("msg: " + msg);
		// Accesso ai campi del messaggio
		 String content = msg.msgContent();
		 System.out.println("Message Content: " + content);
		 
		 String mjson = msg.toJsonString();
		 System.out.println("mjson: " + mjson);
		 
		 IApplMessage m = ApplMessage.cvtJson( mjson );
		 System.out.println("m: " + m );
	}
	public static void main(String[] args) {
		new ApplMessageUsage().doJob();

	}

}
