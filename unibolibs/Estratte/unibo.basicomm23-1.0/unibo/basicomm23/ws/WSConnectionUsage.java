package unibo.basicomm23.ws;

import java.util.Observable;
import unibo.basicomm23.interfaces.IObserver;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtilsOrig;
import unibo.basicomm23.utils.ConnectionFactory;


/*
 *
 */
//@ClientEndpoint
public class WSConnectionUsage implements IObserver{
    private Interaction conn;
    
    public WSConnectionUsage() {
        try {     	
        	conn = ConnectionFactory.createClientSupport(
        			ProtocolType.ws, "localhost:8080", "eval");
        	 
	        CommUtilsOrig.outcyan("WSConnectionUsage on 8080" );        
	        ((WsConnection) conn).addObserver(this);
        } catch (Exception e) {
        	CommUtilsOrig.outred("WSConnectionUsage | ERROR:" +e.getMessage());
        }    	   	
    }

     
    public void workWithGame( ) {
        try {
        	conn.forward("cell(1,2,1)");
//        	conn.forward("cell-2-2");
//        	conn.forward("cell-3-2");
//        	CommUtils.delay(3000); 
//            conn.forward("start");           
//            CommUtils.delay(3000);            
//            conn.forward("stop");
         } catch (Exception e) {
        	CommUtilsOrig.outred("WSConnectionUsage | ERROR:" +e.getMessage());
        }    	
    }
    
	@Override 
	public void update(Observable o, Object arg) {
		//CommUtils.outyellow("WSConnectionUsage | riceve da observale: " + o + " la info:" + arg);		
		update(arg.toString() );
	}


	@Override
	public void update(String message) {
		CommUtilsOrig.outgreen("ConwayCallerWs | update elabora: " + message);
	}
 

    public static void main(String[] args) {
    	WSConnectionUsage caller = new WSConnectionUsage();
    	caller.workWithGame(); 
     	CommUtilsOrig.delay(10000); //To chcek broadcasted messages
    	CommUtilsOrig.outmagenta("WSConnectionUsage | BYE" );
    }







} 