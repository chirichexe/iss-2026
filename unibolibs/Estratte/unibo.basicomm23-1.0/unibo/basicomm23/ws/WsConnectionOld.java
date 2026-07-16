package unibo.basicomm23.ws;

import java.net.URI;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import unibo.basicomm23.interfaces.IObservable;
import unibo.basicomm23.interfaces.IObserver;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.CommUtilsOrig;
import unibo.basicomm23.utils.Connection;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.msg.ApplMessage;

public class WsConnectionOld extends Connection implements  IObservable{
	private static HashMap<String,WsConnectionOld> connMap= new HashMap<String,WsConnectionOld>();
	private Vector<IObserver> observers                = new Vector<IObserver>();
 	private  WebSocketClient client;
 	private BlockingQueue<String> replyQueue = new LinkedBlockingDeque<String>(50);
	
	public static WsConnectionOld create(String addr, String entry ) throws Exception{
//		CommUtils.outyellow("             WsConnection | create addr=" + addr + " alreadY:" + connMap.containsKey(addr)  );
		if( ! connMap.containsKey(addr)){
			connMap.put(addr, new WsConnectionOld( addr,entry ) );
		}else {
			CommUtilsOrig.outyellow("             WsConnection | ALREADY connected to addr=" + addr  );
		}
		return connMap.get(addr);
	}

	public static WsConnectionOld create(String addr, String entry , IObserver obs ) throws Exception{
		WsConnectionOld conn = create(addr, entry);
		conn.addObserver(  obs  );
		return conn;
	}
	
	public boolean isOpen() {
		return client.isOpen();
	}
	
	private WsConnectionOld( String addr, String endpoint  ) throws Exception {
		//trace = true;
		if( endpoint.length() > 0 ) wsconnect(addr+"/"+endpoint );
		else wsconnect(addr);
	}
	
    private void wsconnect(String wsAddr) throws Exception{    // localhost:8091
        try {
//        	if( trace ) CommUtilsOrig.outyellow("             WsConnection | wsconnect wsAddr=" + wsAddr);
			URI uri = new URI("ws://"+wsAddr);
			CommUtilsOrig.outyellow("             WsConnection | wsconnect uri=" + uri);
			if( trace ) CommUtilsOrig.outyellow("             WsConnection | wsconnect to uri=" + uri  );
		    
			client = new WebSocketClient(uri) {

				@Override
				public void onOpen(ServerHandshake handshakedata) {
					if( trace )  
						CommUtilsOrig.outyellow("             WsConnection | opening websocket" ); //userSession="+userSession.getRequestURI()
				}

				@Override
				public void onMessage(String message) {
			    	if( trace ) {
			    		CommUtilsOrig.outmagenta("             WsConnection | onMessage:" + message );
			    		CommUtils.aboutThreads("onMessage ----------- "); //WebSocketConnectReadThread-15
			    	}
 					checkIfAnswer(message); //notifico se non reply
 				}			

				@Override
				public void onClose(int code, String reason, boolean remote) {
					if( trace ) 
						CommUtilsOrig.outyellow("             WsConnection | closing. userSession=" );
				}

				@Override
				public void onError(Exception ex) {
					CommUtilsOrig.outred("             WsConnection | onError " + ex.getMessage());
				}
	    	
	        };         
	        client.connect();
    		while( ! client.isOpen() ) {
    			//if( trace ) 
    		    CommUtilsOrig.outblue("             WsConnection | waiting client connected ...");
    			CommUtilsOrig.delay(100);
    		}
	        if( ! client.isOpen() ) { throw new Exception(" WsConnection | client KO to " + wsAddr); }
			if( trace ) CommUtilsOrig.outmagenta	("             WsConnection | wsconnected to wsAddr=" + wsAddr  );
        } catch ( Exception ex) {
        	CommUtils.outred("             WsConnection | wsconnect ERROR: " + ex.getMessage());
        	throw new Exception("WsConnection | " + ex.getMessage());
        }
    }	

	protected void checkIfAnswer(String message) {
		try {
			IApplMessage reqMsg = new ApplMessage(message);
			if( reqMsg.isReply() ) {
				//CommUtilsOrig.outgreen("             WsConnection | checkIfAnswer msg IS reply " +  message);
				replyQueue.put(message);  //sblocca
				return;
			}else {
				CommUtilsOrig.outmagenta("             WsConnection | checkIfAnswer msg is not a reply " +  message);
			}
		}catch( Exception e) {
			//CommUtilsOrig.outmagenta("             WsConnection | checkIfAnswer msg is not IApplMessage " +  message);
		}
		//Propago a tutti gli observer della connessione il messaggio inviato dal server
		//CommUtilsOrig.outmagenta("             WsConnection | updateObservers"  );
		updateObservers(message); 
	}

    protected void updateObservers( String msg ){
		if( trace ) CommUtilsOrig.outyellow("             WsConnection | updateObservers " + observers.size()  );
        observers.forEach( v -> v.update(null, msg) );
    }


	@Override
	public void addObserver(IObserver obs) {
        CommUtilsOrig.outyellow("             WsConnection | addObserver " + obs  );
		observers.add( obs);
	}

	@Override
	public void deleteObserver(IObserver obs) {
        CommUtilsOrig.outyellow("             WsConnection | deleteObserver " + obs  );
		observers.remove( obs);
	}

	@Override
	public void forward(String msg) throws Exception {
		if( trace ) CommUtilsOrig.outcyan("             WsConnection | forward " + msg );
		sendMessageSynch(msg);
	}

	@Override
	public String request(String msg) throws Exception {
		if( trace ) 
			CommUtilsOrig.outblack("             WsConnection | request "  + msg );  //main
		IApplMessage reqMsg = new ApplMessage(msg); //just to check ...
		forward(msg); //ogni connessione ha il suo client
		//CommUtils.aboutThreads("request");		
		String reply = replyQueue.take();//blockig until reply received		
		//CommUtilsOrig.outgreen("             WsConnection | RESUMING:" + reply);		
  		return reply;
	}

	@Override
	public void reply(String msgJson) throws Exception {
		forward(msgJson);
	}

	@Override
	public String receiveMsg() throws Exception {
		throw new Exception(" WsConnection | receiveMsg not implemented for WS ");
	}
	

	@Override
	public void close() throws Exception {
		if( trace ) CommUtilsOrig.outyellow("             WsConnection | close gracefulShutdown=" );
        client.closeBlocking(); 
        //close() : Initiates the websocket close handshake. This method does not block
        //In oder to make surethe connection is closed use closeBlocking
	}

    public void sendMessageSynch(String message ) throws Exception {
//		 CommUtils.outblack("             WsConnection | sendMessageSynch " + message  + " endpoint=" + endpoint );
    	if( trace ) CommUtilsOrig.outblack("             WsConnection | client " +  client );
		try{
			client.send(  message);//blocks until the message has been transmitted
		}catch(Exception e){
		   CommUtilsOrig.outred("             WSConnection | sendMessageSynch " + message + " ERROR:" + e.getMessage());
		}
    }
//	public void sendMessageAsynch(String message ) throws Exception {
//		if( trace ) CommUtils.outmagenta("             WsConnection | sendMessageSynch " + message + " userSession=" + userSession);
//		//client.getAsyncRemote().sendText(endpoint+"/"+ message);
//	}

}
