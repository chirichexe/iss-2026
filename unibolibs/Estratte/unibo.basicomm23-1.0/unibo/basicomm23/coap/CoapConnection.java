package unibo.basicomm23.coap;

import org.eclipse.californium.core.CoapClient;
import org.eclipse.californium.core.CoapHandler;
import org.eclipse.californium.core.CoapObserveRelation;
import org.eclipse.californium.core.CoapResponse;
import org.eclipse.californium.core.coap.MediaTypeRegistry;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.utils.CommUtilsOrig;
import unibo.basicomm23.utils.Connection;
import java.nio.charset.StandardCharsets;
import java.net.URLEncoder;
import java.net.URLDecoder;

public class CoapConnection extends Connection {
protected CoapClient client;  //protected : Barbieri 2023
protected String url;
private String answer = "unknown";

	public static Interaction create(String host, String path) throws Exception {
	 	return new CoapConnection(host,path);
	}

	public CoapConnection( String address, String path) {
		//"coap://localhost:5683/" + path
 		setCoapClient(address,path);
	}

	public String toString(){
		return url;
	}
	
	public CoapClient getClient() {
		return client;
	}
	protected void setCoapClient(String addressWithPort, String path) {
		if( trace ) CommUtilsOrig.outmagenta(  "    +++ CoapConn | setCoapClient addressWithPort=" +  addressWithPort  );
		//url            = "coap://"+address + ":5683/"+ path;
		url            = "coap://"+addressWithPort + "/"+ path;
		if( trace )  CommUtilsOrig.outyellow(  "    +++ CoapConn | setCoapClient url=" +  url  );
		client          = new CoapClient( url );
 		client.useExecutor(); //To be shutdown
		if( trace )  CommUtilsOrig.outyellow("    +++ CoapConn | STARTS client url=" +  url ); //+ " client=" + client );
		//client.setTimeout( 1000L );   //TRoPPO POCO: i servizi potrebbero richiedere tempo ...
		client.setTimeout(30000L);   //OCT24
	}
 	
	public void removeObserve(CoapObserveRelation relation) {
		relation.proactiveCancel();
		if( trace )  CommUtilsOrig.outyellow("    +++ CoapConn | removeObserve " + relation   );
	}
	public CoapObserveRelation observeResource( CoapHandler handler  ) {
		CoapObserveRelation relation = client.observe( handler ); 
		//if( trace )  CommUtils.outyellow("    +++ CoapConn |  added " + handler + " relation=" + relation + relation );
 		return relation;
	}


	
//From Interaction - SIAMO LATO CLIENT

	/*
	PRIMA DI INVIARE, la connessione CODIFICA il MSG
	 */
	@Override
	public void forward(String msg)   {
	    if( trace )
	    	CommUtilsOrig.outyellow(  "    +++ CoapConn | forward " + url + " msg=" + msg );

		if( client != null ) {
			try {
				msg = URLEncoder.encode(msg, StandardCharsets.UTF_8.toString());
				//CommUtils.outyellow(  "    +++ CoapConn | forward encoded msg=" + msg );
				CoapResponse resp = client.put(msg, MediaTypeRegistry.TEXT_PLAIN); //Blocking!
				//CommUtils.outyellow(  "    +++ CoapConn | forward resp=" + resp );
				if (resp != null) {
					answer = resp.getResponseText();
					if (trace)
						CommUtilsOrig.outyellow("    +++ CoapConn | forward " + msg + " answer=" + answer);
				} else {
					CommUtilsOrig.outred("    +++ CoapConn | forward - resp null for " + msg);
				}  //?????
			}catch( Exception e){
				CommUtilsOrig.outred("    +++ CoapConn | forward - encode ERROR " + e.getMessage());

			}
		} 
	}

	/*
		PRIMA DI INVIARE, la connessione CODIFICA il MSG
		LA RISPOSTA VIENE DECODIFICSTA
	 */
	
	@Override
	public String request(String query) {
		if( trace )
			CommUtilsOrig.outyellow("    +++ CoapConn | request query=" + query + " url=" + url);
		try {
			//Il contenuto del msg è già encoded da chi chiama
 			if ( !query.isEmpty() ) query = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());
			//CommUtils.outyellow("    +++ CoapConn | request query encoded=" + query);

			String param = query.isEmpty() ? "" : "?q=" + query;
			if( trace )
				CommUtilsOrig.outcyan("    +++ CoapConn | request param=" + (url + param));

			client.setURI(url + param); //JAN2024

			//Risponde handlePUT di un actor SIAMO LATO CLIENT
			CoapResponse response = client.put(query, MediaTypeRegistry.TEXT_PLAIN); //FEB2023
			//OPPURE ( FEB2023 )
			//client.setURI(url );
			//CoapResponse response = client.put(query, MediaTypeRegistry.TEXT_PLAIN);

			if (response != null) {
				if( trace )
					CommUtilsOrig.outyellow("    +++ CoapConn | request=" + query
						+ " RESPONSE CODEEEE: " + response.getCode() + " answer=" + response.getResponseText());
				//DECODE response.getResponseText()?
				String responseDecoded =
						URLDecoder.decode(response.getResponseText(), StandardCharsets.UTF_8.toString() );

				//CommUtils.outmagenta("    +++ CoapConn | responseDecoded=" + responseDecoded );

				return responseDecoded;  //VA A Connection
			} else {
				CommUtilsOrig.outred("    +++ CoapConn | request=" + query + " RESPONSE NULL ");
				return null;
			}
		} catch (Exception e) {
			CommUtilsOrig.outred("    +++ CoapConn | request - encode ERROR " + e.getMessage());
			return null;
		}
	}
	
	//https://phoenixnap.com/kb/install-java-raspberry-pi
	
	@Override
	public void reply(String reqid) throws Exception {
		throw new Exception( "   +++ CoapConn | reply not allowed");
	} 

	@Override
	public String receiveMsg() throws Exception {
		if( trace )  CommUtilsOrig.outyellow(  "    +++ CoapConn | receiveMsg" );
		while( answer.equals( "unknown" ) ){ //FEB2023
			Thread.sleep(500);
			CommUtilsOrig.outyellow(  "    +++ CoapConn | waiting for answer ..." );
		}
		return answer;
	}

	@Override
	public void close()  {
		if( trace ) CommUtilsOrig.outyellow(  "    +++ CoapConn | client shutdown=" + client);
		client.shutdown();	
	}

}
/*
Log4j by default looks for a file called log4j.properties or log4j.xml on the classpath
System.setProperty(org.slf4j.impl.SimpleLogger.DEFAULT_LOG_LEVEL_KEY, "ERROR");
*/