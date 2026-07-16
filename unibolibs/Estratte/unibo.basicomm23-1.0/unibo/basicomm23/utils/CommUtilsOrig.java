package unibo.basicomm23.utils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.LoggerFactory;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.MyDataCenterInstanceConfig;
import com.netflix.discovery.DefaultEurekaClientConfig;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;

import unibo.basicomm23.eureka.EurekaUniboUtils;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.msg.ApplMessageType;
import unibo.basicomm23.msg.ProtocolType;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.io.PrintWriter;

//import ch.qos.logback.classic.Logger;
//import ch.qos.logback.classic.LoggerContext;
//import net.logstash.logback.appender.LogstashTcpSocketAppender;
//import net.logstash.logback.encoder.LogstashEncoder;

public class CommUtilsOrig {
	private static JSONParser simpleparser = new JSONParser();
 	
	public static boolean isCoap() {
		return CommSystemConfig.protcolType==ProtocolType.coap ;
	}
	public static boolean isMqtt() {
		return CommSystemConfig.protcolType==ProtocolType.mqtt ;
	}
	public static boolean isTcp() {
		return CommSystemConfig.protcolType==ProtocolType.tcp ;
	}
	
	public static String getContent( String msg ) {
		String result = "";
		try {
			ApplMessage m = new ApplMessage(msg);
			result        = m.msgContent();
		}catch( Exception e) {
			result = msg;
		}
		return result;	
	}
	
	public static JSONObject parseForJson(String message) {
	   try {
 	       JSONObject jsonObj = (JSONObject) simpleparser.parse(message);
	       return jsonObj;
	   } catch (Exception e) {
	       //CommUtils.outred("CommUtils | parseJson ERROR:"+e.getMessage());
	       return null;
	   }
	}

	
	//String MSGID, String MSGTYPE, String SENDER, String RECEIVER, String CONTENT, String SEQNUM
	private static int msgNum=0;	

	public static IApplMessage buildDispatch(String sender, String msgId, String payload, String dest) {
		try {
			return new ApplMessage(msgId, ApplMessageType.dispatch.toString(),sender,dest,payload,""+(msgNum++));
		} catch (Exception e) {
			ColorsOut.outerr("buildDispatch ERROR:"+ e.getMessage());
			return null;
		}
	}
	
	public static IApplMessage buildRequest(String sender, String msgId, String payload, String dest) {
		try {
			return new ApplMessage(msgId, ApplMessageType.request.toString(),sender,dest,payload,""+(msgNum++));
		} catch (Exception e) {
			ColorsOut.outerr("buildRequest ERROR:"+ e.getMessage());
			return null;
		}
	}
	public static IApplMessage buildReply(String sender, String msgId, String payload, String dest) {
		try {
			return new ApplMessage(msgId, ApplMessageType.reply.toString(),sender,dest,payload,""+(msgNum++));
		} catch (Exception e) {
			ColorsOut.outerr("buildRequest ERROR:"+ e.getMessage());
			return null;
		}
	}	
	public static IApplMessage prepareReply(IApplMessage requestMsg, String answer) {
		String sender  = requestMsg.msgSender();
		String receiver= requestMsg.msgReceiver();
		String reqId   = requestMsg.msgId();
		IApplMessage reply = null;
		if( requestMsg.isRequest() ) { //DEFENSIVE
			//The msgId of the reply must be the id of the request !!!!
 			reply = buildReply(receiver, reqId, answer, sender) ;
		}else { 
			ColorsOut.outerr( "Utils | prepareReply ERROR: message not a request");
		}
		return reply;
    }


	public static IApplMessage buildEvent( String emitter, String msgId, String payload ) {
		try {
			return new ApplMessage(msgId, ApplMessageType.event.toString(),emitter,"ANY",payload,""+(msgNum++));
		} catch (Exception e) {
			ColorsOut.outerr("buildEvent ERROR:"+ e.getMessage());
			return null;
		}
	}
	public static void outyellow( String msg ) { ColorsOut.outappl(msg, ColorsOut.YELLOW); }
	public static void  outgreen( String msg ) { ColorsOut.outappl(msg, ColorsOut.GREEN); }
	public static void  outblue( String msg ) {  ColorsOut.outappl(msg, ColorsOut.BLUE);  }
	public static void  outred(String msg) { ColorsOut.outappl(msg, ColorsOut.RED) ; }
	public static void  outblack(String msg) { ColorsOut.outappl(msg, ColorsOut.BLACK) ; }
	public static void  outcyan(String msg) { ColorsOut.outappl(msg, ColorsOut.CYAN) ; }
	public static void  outmagenta(String msg) {  ColorsOut.outappl(msg, ColorsOut.MAGENTA); }
	public static void  outgray(String msg) { ColorsOut.outappl(msg, ColorsOut.GRAY) ; }

	public static void delay( int dt ) {
		try {
			Thread.sleep(dt);
		} catch (InterruptedException e) {
				e.printStackTrace();
		}		
	} 
	
	public static void aboutThreads(String msg)   { 
		String tname    = Thread.currentThread().getName();
		String nThreads = ""+Thread.activeCount() ;
		outcyan( msg + " curthread=T n=N".replace("T", tname).replace("N", nThreads) );
	}

	public static void forwardOnInterconn( Interaction conn,String cmd )  {
		//CommUtils.outblue( name+"  | forwardOnInterconn " + cmd + " conn=" + conn );
		try {
			conn.forward(cmd);
		} catch (Exception e) {
			CommUtilsOrig.outred(  "CommUtils | forwardOnInterconn ERROR=" + e.getMessage()  );
		}
	}
	public static void replyOnInterconn( Interaction conn,String cmd )  {
		//CommUtils.outblue( name+"  | replyOnInterconn " + cmd + " conn=" + conn );
		try {
			conn.reply(cmd);
		} catch (Exception e) {
			CommUtilsOrig.outred(  "CommUtils | replyOnInterconn ERROR=" + e.getMessage()  );
		}
	}
	public static String requestSynchOnInterconn(Interaction conn, String request )  {
		//CommUtils.outblue( name+"  | requestOnInterconn request=" + request + " conn=" + conn );
		try {
			String answer = conn.request(request);
			//CommUtils.outblue( name+"  | requestOnInterconn-answer=" + answer  );
			return  answer  ;
		} catch (Exception e) {
			CommUtilsOrig.outred(  "CommUtils  | requestOnInterconn ERROR=" + e.getMessage()  );
			return null;
		}
	}
	public static void waitTheUser(String msg) {
		try {
			int v = -1;
			while( v == -1 ) {
			    outblue(msg);
				v = System.in.read();
				delay(2500);
			}
			System.in.read(); //discard CR
			//outblue("CommUtils v="+v);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public static void waitTheUserqak(String msg) {
//		new Thread() {
//			public void run() {
//				outblue("CommUtils waitTheUserqak "+ msg);
//				waitTheUser(  msg );
//			}
//		}.start();
//	}

	public static void beep(){
		java.awt.Toolkit.getDefaultToolkit().beep();
		//System.out.print("\007");// ASCII bell
		// System.out.flush();
	}

	public static String convertToSend(String s){
		return "'"+s.replace("\n","@!@")+"'";
	}
	public static String restoreFromConvertToSend(String s){
		return s.replace("@!@","\n");
	}

   //Sept 2024
	public static void clearlog( String fName){
		try{
			PrintWriter p = new PrintWriter(fName);
			p.write("");
			p.close();
		}catch(Exception e){
			outred("CommUtils | clearlog ERROR:"+ e.getMessage());
		}
	}

	/*
	OCT24
	 */
	
	public static String toPrologStr(String s, boolean on){
		try{
			if (! on) ///tolgo gli apici;
			return "'" + s.replace("'","");  //tolgo gli apici;
		else {
			new org.json.simple.parser.JSONParser().parse(s); //UN oggetto JSON è ok
			return "'"+s+"'";
		}
		}catch(Exception e){ //NON JSON
			try{
				alice.tuprolog.Term.createTerm(s); //UN termione prolog è ok
 				return s;
			}catch(Exception e1) {  //Stringa qualsiasi, ad ese. con bianchi
				//CommUtils.outred("toPrologStr not a term")
				return "'"+s+"'";
			}
        }					
	}
	
	public static String getMyPublicip(){
		try {
			// URL di un servizio che restituisce l'indirizzo IP pubblico
			String serviceUrl = "https://checkip.amazonaws.com";

			// Creazione della connessione HTTP
			URL url = new URL(serviceUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");

			// Lettura della risposta
			BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String inputLine;
			StringBuilder response = new StringBuilder();

			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}

			in.close();

			// Stampa dell'indirizzo IP pubblico
			String myip = response.toString().trim();
			//outcyan("Il tuo indirizzo IP pubblico è: " +  myip);
		    return  myip;
		} catch (Exception e) {
			outred("Errore nell'ottenere l'indirizzo IP: " + e.getMessage());
			return null;
		}
	}


	public static boolean ckeckEureka(){  //LEGACY ...
		return EurekaUniboUtils.checkEureka();
	}
	
	public static boolean checkEureka(){
		return EurekaUniboUtils.checkEureka();
	}

	public static DiscoveryClient createEurekaClient( ) {
		return EurekaUniboUtils.createEurekaClient(); 
 	}
	public static DiscoveryClient createEurekaClient( EurekaInstanceConfig config ) {
		return EurekaUniboUtils.createEurekaClient( config );
 	}

	public static DiscoveryClient registerService(  EurekaInstanceConfig config ) {
		return EurekaUniboUtils.registerTheServiceOnEureka( config );
 	}
	
	public static String[] discoverService(EurekaClient eurekaClient, String serviceName){
		return EurekaUniboUtils.discoverService(eurekaClient, serviceName);
	}

	public static String[] discoverService( String serviceName ){
		return EurekaUniboUtils.discoverService( serviceName );
	}
	
	public static String getEnvvarValue(String envvarName) {
		return System.getenv(envvarName);
	}
	
	//ADDED APril2025
	
	public static String getServerLocalIp() {		
        try {
            Enumeration<NetworkInterface> interfacce = NetworkInterface.getNetworkInterfaces();
            while (interfacce.hasMoreElements()) {
                NetworkInterface interfaccia = interfacce.nextElement();
                Enumeration<InetAddress> indirizzi = interfaccia.getInetAddresses();
                while (indirizzi.hasMoreElements()) {
                    InetAddress indirizzo = indirizzi.nextElement();
                    if (!indirizzo.isLoopbackAddress()) { // Esclude l'indirizzo loopback (127.0.0.1)
                        //System.out.println("CommUtils "+ interfaccia.getDisplayName() + ": " + indirizzo.getHostAddress());                        
                        if( indirizzo.getHostAddress().startsWith("192.168")) {
                        	//System.out.println("ConwayGuiControllerLifeLocal ==== " + indirizzo.getHostAddress());
                        	return indirizzo.getHostAddress();
                        }
                    }
                }
            }
            return null;
        } catch (SocketException e) {
            outred("Errore durante la ricerca degli indirizzi IP: " + e.getMessage());
            return null;
        }			
 	
	}	

	/*
	public static void loggerStashConfig(String logstashHost, int logstashPort) {
	    //org.slf4j.Logger logger = LoggerFactory.getLogger(CommUtils.class);
		try {
	        outcyan("1) loggerStashConfig " + logstashHost + " " + logstashPort);
		    
	        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
	
	        // Crea un LogstashTcpSocketAppender per inviare i log a Logstash
	        LogstashTcpSocketAppender logstashAppender = new LogstashTcpSocketAppender();
	        logstashAppender.setName("LOGSTASH");
	        logstashAppender.setContext(context);
	        //logstashAppender.addDestination(logstashHost + ":" + logstashPort);
	        outcyan("2) loggerStashConfig " + logstashAppender);
	        
	        logstashAppender.setRemoteHost(logstashHost);
	        logstashAppender.setPort(logstashPort);
	        outcyan("3) loggerStashConfig " + logstashHost + " " + logstashPort);
	
	        // Configura il LogstashEncoder per inviare i log in formato JSON
	        LogstashEncoder logstashEncoder = new LogstashEncoder();
	        logstashAppender.setEncoder(logstashEncoder);
	        outcyan("4) loggerStashConfig " + logstashHost + " " + logstashPort);
	
	        // Attiva l'appender
	        logstashAppender.start();
	        outcyan("5) loggerStashConfig start "  );
	
	        // Aggiungi l'appender al root logger
	        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
	        rootLogger.addAppender(logstashAppender);
	        outcyan("6) loggerStashConfig start "  );
	
	        outcyan(" ....................... Logstash Appender configurato per l'host {} e porta {} " + logstashHost + " " + logstashPort);
		} catch (Exception e) {
			outred("loggerStashConfig ERROR: " + e.getMessage());
		}
	}
  
	*/
	
}
