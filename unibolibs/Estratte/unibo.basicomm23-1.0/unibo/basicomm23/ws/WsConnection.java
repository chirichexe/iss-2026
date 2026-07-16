package unibo.basicomm23.ws;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingDeque;
//import org.java_websocket.client.WebSocketClient;
//import org.java_websocket.handshake.ServerHandshake;
//import conway26appl.caller.WsConnection.WebSocketListener;
import unibo.basicomm23.interfaces.IObservable;
import unibo.basicomm23.interfaces.IObserver;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.CommUtilsOrig;
import unibo.basicomm23.utils.Connection;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.msg.ApplMessage;

public class WsConnection extends Connection implements  IObservable{
//	private static HashMap<String,WsConnection> connMap= new HashMap<String,WsConnection>();
	
	private Map<String, WebSocket> activeConnections  = new ConcurrentHashMap<>();
	private Vector<IObserver> observers                = new Vector<IObserver>();
 	private HttpClient client                          = HttpClient.newHttpClient(); 
 	private String endpoint                            = "todo";
 	private BlockingQueue<String> replyQueue = new LinkedBlockingDeque<String>(50);
 	private CountDownLatch latch             = new CountDownLatch(1); //Inizializzo a 1 perché aspetto UNA risposta dal server
 	private WebSocket webSocket;
	
 	/*
 	 * Ll'HttpClient è asincrono e thread-safe per natura;
 	 * si possono gestire decine di WebSocket contemporaneamente usando un unico client.
 	 * 
 	 */
	public static Interaction create(String addr, String entry ) throws Exception{
 		//CommUtils.outyellow("             WsConnection | create addr=" + addr + " alreadY:" + connMap.containsKey(addr)  );
//		if( ! connMap.containsKey(addr)){
//			connMap.put(addr, new WsConnection( addr,entry ) );
//		}else {
//			CommUtilsOrig.outyellow("             WsConnection | ALREADY connected to addr=" + addr  );
//		}
//		return connMap.get(addr);
		return new WsConnection( addr,entry );
	}

	public static Interaction create(String addr, String entry , IObserver obs ) throws Exception{
		Interaction conn = create(addr, entry);
		if( obs != null ) ((IObservable) conn).addObserver(  obs  );
		return conn;
	}
	
	public boolean isOpen() {
		return ! webSocket.isInputClosed();
	}
	
	private WsConnection( String addr, String endpoint  ) throws Exception {
		if( endpoint.length() > 0 ) {
			this.endpoint = endpoint;
			wsconnect(addr+"/"+endpoint );
		}
		else wsconnect(addr);
	}
	
    private void wsconnect(String wsAddr) throws Exception{    // wsAddr : localhost:8091
        try {
        	webSocket = client.newWebSocketBuilder()
            .buildAsync(URI.create("ws://"+wsAddr), new WebSocketListener(latch,replyQueue))
            .join();
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
        //CommUtilsOrig.outyellow("             WsConnection | addObserver " + obs  );
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
		CommUtilsOrig.outred("             WsConnection | WARNING no close for java.net.http.HttpClient" );
        //client.;  //NON ESISTE CLOSE
		 
	}

    public void sendMessageSynch(String message ) throws Exception {
    	if( trace ) CommUtils.outblack("             WsConnection | sendMessageSynch " + message  + " endpoint=" + endpoint );
 		try{
			 webSocket.sendText(message, true);
			 //latch.await();
		}catch(Exception e){
		   CommUtilsOrig.outred("             WSConnection | sendMessageSynch " + message + " ERROR:" + e.getMessage());
		}
    }

    /*
     * onClose del WebSocket.Listener verrà attivato solo se l'intero tunnel cade. 
     * Se un singolo client remoto termina, il server non chiuderà il socket, 
     * ma invierà un messaggio applicativo (un JSON o un byte array) per dirtelo.
     * 
     */
    
    private  class WebSocketListener implements WebSocket.Listener {
        private final CountDownLatch latch;
        private final BlockingQueue<String> replyQueue;
        private String msg;
        
        public WebSocketListener(CountDownLatch latch, BlockingQueue<String> replyQueue) {
            this.latch      = latch;
            this.replyQueue = replyQueue;
        }

        @Override
        public void onOpen(WebSocket webSocket) {
            System.out.println("WsConnection WebSocketListener | --- Connessione aperta ---");
            webSocket.request(1); // Importante: richiede il primo messaggio
            //WebSocket.Listener.super.onOpen(webSocket);
        }
   
        /*
         * I metodi del listener restituiscono un CompletionStage. 
         * Questo permette di gestire messaggi molto grandi o operazioni lente in modo non bloccante. 
         * Di default, richiamare super.onText è sufficiente.
         * 
         */
        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
             	if(trace) 
            		CommUtils.outmagenta("WsConnection WebSocketListener | Messaggio ricevutoooo dal server: " + data);
            	msg = data.toString();
            	if( msg.startsWith("msg(")) {  //perhaps a IApplMessage
//                    try {
		            	IApplMessage m = new ApplMessage(data.toString());
		            	if( m.isReply() ) {
			            	//CommUtils.outred("WsConnection WebSocketListener | put in queue " + m );
			            	try {
								replyQueue.put(m.toString());
							} catch (Exception e) {
								CommUtils.outred("WsConnection | WebSocketListener Error " + e.getMessage());
							}
		            	}else if(m.msgId().equals("endremoteclient")) {
		            		CommUtils.outred("WsConnection WebSocketListener | endremoteclient TODO:pulire"  );
		            		//protocollo di "pulizia" per evitare memory leak o messaggi fantasma che vagano nel sistema
		            	}
//		            }catch(Exception e) {
//		            	CommUtils.outyellow("WsConnection WebSocketListener |  WARNING " + e.getMessage() );
//		            }
            }
            //CommUtils.outmagenta("WsConnection WebSocketListener | observers " + observers.size());
            observers.forEach( v -> v.update(null, data.toString()) );
            return WebSocket.Listener.super.onText(webSocket, data, last);
            //non fa "nulla" di operativo, ma serve a gestire il flusso dei dati (backpressure).
            /*
             * super.onText(...) dice: "Esegui l'implementazione predefinita prevista dai 
             * progettisti di Java per questo metodo" che fa essenzialmente due cose:

				Richiede il messaggio successivo: Segnala al sistema che il listener è 
				pronto a ricevere altri dati.
				
				Restituisce null (o un CompletionStage già completato): 
				Indica che l'elaborazione del messaggio corrente è terminata e 
				non ci sono operazioni asincrone in sospeso.
				
				onText non restituisce un void, ma un CompletionStage<?>. 
				Questo serve per la Backpressure (gestione del carico):
				
				- Se restituisci super.onText(...): Dichiari che hai finito di leggere. 
				  Il WebSocket continuerà a inviarti messaggi non appena arrivano dal buffer di rete.

				- Se volessi fare un'operazione lenta: Potresti restituire un tuo CompletableFuture. 
				  Il WebSocket aspetterebbe che quel futuro sia completato prima di invocare 
				  nuovamente onText per il messaggio successivo. 
				  Questo evita che il server "sommerga" il tuo client di dati 
				  se il tuo codice è lento a elaborarli 
				  (ad esempio, se devi ricalcolare tutta la griglia di Conway 
				   prima di passare al turno successivo).
 
             */
        }//onText

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            System.out.println("WsConnection WebSocketListener | --- Connessione chiusa reaosn=" + reason + " ---");
//            if( latch != null ) latch.countDown();
            // È importante restituire null o un completamento per confermare la ricezione
            return null; //WebSocket.Listener.super.onClose(webSocket, statusCode, reason);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            CommUtils.outred("WsConnection WebSocketListener | onError:" + error.getMessage() + " msg="+msg);
            //            if( latch != null ) latch.countDown();
//            if(error.getMessage().contains("tuprolog")) {
//            	observers.forEach( v -> v.update(null, msg) );
//            }
        }
    }
   
}
