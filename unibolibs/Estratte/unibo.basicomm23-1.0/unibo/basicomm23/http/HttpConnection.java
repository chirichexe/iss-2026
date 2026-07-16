package unibo.basicomm23.http;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.utils.CommUtilsOrig;
import unibo.basicomm23.utils.Connection;
import unibo.basicomm23.utils.SystemTimer;


//https://hc.apache.org/httpcomponents-client-4.5.x/current/tutorial/pdf/httpclient-tutorial.pdf
public class HttpConnection extends Connection {// implements Interaction2021 {
private static HashMap<String, HttpConnection> connMap= new HashMap<String, HttpConnection>();
private HttpClient client =  HttpClients.createDefault();
private  String URL;
private SystemTimer timerHttp   = new SystemTimer();
private JSONParser simpleparser = new JSONParser();
//final MediaType JSON_MediaType     = MediaType.get("application/json; charset=utf-8");


public static Interaction create(String addr ){
	CommUtilsOrig.outyellow("HttpConnection | create " + addr);
	if( ! connMap.containsKey(addr)){
		connMap.put(addr, new HttpConnection( addr ) );
	}
	return connMap.get(addr);
}

	public HttpConnection(String url) {
		URL = "http://" +url;
		CommUtilsOrig.outyellow("HttpConnection | create with URL=" + URL );
    }

//Since inherits from Interaction2021 
	@Override
	public void forward( String msg) throws Exception {
		CommUtilsOrig.outyellow("HttpConnection | forward:" + msg  );
      //String answer = sendHttp( msg );
      String answer = makeCall( URL, "", "POST", "application/json", msg);
      CommUtilsOrig.outyellow("HttpConnection | answer=" + answer );
	} 

	@Override
//	public String request(String msg) throws Exception {
//		CommUtils.outred("HttpConnection request " + msg + " URL=" + URL);
//		String answer = makeCall( URL, "", "GET", "application/json", msg);
//		return answer; //callHTTPforstr( msg ); //DEC2024
//	}
	public String request(String msg) throws Exception {
//		CommUtilsOrig.outred("HttpConnection request " + msg + " URL=" + URL);
//		String answer = makeCall( URL, "", "POST", "application/json", msg);
		return callHTTPforstr( msg ); //MAY2025
	}
	@Override
	public void reply(String msgJson) throws Exception {
		CommUtilsOrig.outred("SORRY: not connected for ws");
		throw new Exception("HttpConnection does not implement reply");
	}

	@Override
	public String receiveMsg() throws Exception {
		throw new Exception("HttpConnection does not implement receiveMsg");
	}

	@Override
	public void close() throws Exception {
	}
	
	
//----------------------------------------------------------------------

  public String sendHttp( String msgJson){
      try {
		  CommUtilsOrig.outgreen("HttpConnection | sendHttp msgJson=" + msgJson + " URL=" + URL);
		  timerHttp.startTime();
          String answer     = "";
//          List<NameValuePair> params = new ArrayList<NameValuePair>();
//          params.add(new BasicNameValuePair("msg", msgJson));
//           params.add(new BasicNameValuePair("\"robotmove\"", "\"turnLeft\""));
//           params.add(new BasicNameValuePair("\"time\"", "\"300\""));
          HttpPost httpPost = new HttpPost( URL );
          httpPost.setEntity(new StringEntity(msgJson));
          HttpResponse response = client.execute(httpPost);          
//          Long res = response.getEntity().getContent().transferTo(System.out);
		  //CommUtils.delay(1000) ; //per permettere alla gui di finire la rotazione ???
		  timerHttp.stopTime();
		  answer=EntityUtils.toString( response.getEntity() );
		  if(trace) CommUtilsOrig.outyellow("HttpConnection | sendHttp answer="+answer+" elapsed="+timerHttp.getDuration());
          return answer;
      }catch(Exception e){
    	  CommUtilsOrig.outred("sendHttp ERROR:" + e.getMessage());
          return "";
      }
  }

	public JSONObject callHTTP(String msg )  {
//		CommUtilsOrig.outyellow("HttpConnection | callHTTP msg=" + msg  + " URL=" + URL);
		JSONObject jsonEndmove = null;
		try {
			StringEntity entity = new StringEntity(msg);
			HttpUriRequest httppost = RequestBuilder.post()
					.setUri(new URI(URL))
					.setHeader("Content-Type", "application/json")
					.setHeader("Accept", "application/json")
					.setEntity(entity)
					.build();
			long startTime        = System.currentTimeMillis() ;
			HttpResponse response = client.execute(httppost);
			long duration  = System.currentTimeMillis() - startTime;
			String answer  = EntityUtils.toString(response.getEntity());
//			CommUtilsOrig.outblue( "HttpConnection | " + Thread.currentThread() + " callHTTP  answer= " + answer + " duration=" + duration );

			jsonEndmove = (JSONObject) simpleparser.parse(answer);  //se riceve pagina HTML darà ERROR
//			CommUtilsOrig.outyellow("callHTTP | jsonEndmove=" + jsonEndmove + " duration=" + duration);
		} catch(Exception e){
			CommUtilsOrig.outred("callHTTP | " + msg + " ERROR:" + e.getMessage());
			try {
				return (JSONObject)  simpleparser.parse( "{\"answer\":\"error\" ,  \"cause\": \"GET\"}" );
			} catch (ParseException e1) {
 				e1.printStackTrace();
			}
		}
		return jsonEndmove;
	} 
	
	//DEC2024
	public String callHTTPforstr( String msg )  {
//		CommUtilsOrig.outgreen("HttpConnection | callHTTPforstr msg=" + msg + " URL=" + URL);
		String answer = callHTTP(msg).toString();
//		try {
//			StringEntity entity = new StringEntity(msg);
//			HttpUriRequest httpget = RequestBuilder.post()
//					.setUri(new URI(URL))
//					.setHeader("Content-Type", "application/json")
//					.setHeader("Accept", "application/json")
//					.setEntity(entity)
//					.build();
//			long startTime        = System.currentTimeMillis() ;
//			HttpResponse response = client.execute(httpget);
//			long duration  = System.currentTimeMillis() - startTime;
//			answer  = EntityUtils.toString(response.getEntity());
//			CommUtils.outyellow( Thread.currentThread() + " callHTTPforstr | answer= " + answer + " duration=" + duration );
//		} catch(Exception e){
//			CommUtils.outred("callHTTPforstr | " + msg + " ERROR:" + e.getMessage());
//		}
		return answer;
	} 

	//DEC2024
	private   String makeCall(String BASE_URL, String endpoint, String method, String contentType, String body) throws IOException {
		URL url = new URL(BASE_URL + endpoint);
		CommUtilsOrig.outyellow("HTTPConnection | makeCall url=" + url + " body=" + body);
		//akeCall url=http://localhost:9111/createProduct body={"productId":2,"name":"p2","weight":20}
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod(method);
		con.setRequestProperty("Content-Type", contentType);

		if (!method.equals("GET")) {
			con.setDoOutput(true);
			DataOutputStream out = new DataOutputStream(con.getOutputStream());
			out.writeBytes(body);
			out.flush();
			out.close();
		}
		
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer content = new StringBuffer();
		while ((inputLine = in.readLine()) != null) {
		    content.append(inputLine);
		}
		in.close();
		
		con.disconnect();
		
		return content.toString();
	}

}
