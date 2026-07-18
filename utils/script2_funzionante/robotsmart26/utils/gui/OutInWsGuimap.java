package gui;

import java.util.Observable;
import unibo.basicomm23.interfaces.IObserver;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.ConnectionFactory;
import unibo.basicomm23.ws.WsConnection;
import java.awt.Desktop;
import java.net.URI;

public class OutInWsGuimap implements IObserver{
	private static OutInWsGuimap instance = null;
    private Interaction conn;
    
    public static OutInWsGuimap getResource(String ip, String port) throws Exception {
    	//CommUtils.outyellow("OutInWsGuimap getResource instance=" + instance);
    	if( instance != null ) return instance;
    	else return new OutInWsGuimap(ip,port);
    }
    
    private OutInWsGuimap(String ip, String port) throws Exception { 
      	
//        try {     	
//        	Runtime.getRuntime().exec("docker-compose -f actorgridgui.yaml up");   	
//        	CommUtils.delay(5000);
//        	CommUtils.outcyan("OutInWsGuimap actorgridgui OPEN http://localhost:"+port);

        	/*
        	 * Desktop.isDesktopSupported() FALSE in docker
        	 */
        	/*
         	if (Desktop.isDesktopSupported()) {
        		CommUtils.outblue("java.awt.Desktop supported ");
        		Desktop desktop = Desktop.getDesktop();
        		URI uri = new URI("http://localhost:8085/"); 
        	    desktop.browse(uri); 
        	}
			*/
        	connectToRobotMind(ip,port); //DEVE ESSERE PARTITA LA FACADE ...
//        } catch (Exception e) {
//        	CommUtils.outred("OutInWsGuimap | ERROR:" +e.getMessage());
//        }    	   	
    }
    
    public void connectToRobotMind(String ip, String port) throws Exception {
//    	try {
        	
//        	CommUtils.outcyan("OutInWsGuimap connectToRobotMind STARTED ---------------------------- " + port);
        	//conn = ConnectionFactory.createClientSupport(ProtocolType.ws, "localhost:"+port, "wsupdates");
        	if( CommUtils.getEnvvarValue("VIRTUAL_ENV") != null) { //In docker ...
        		CommUtils.delay(4000); //Gve time to start the gui
        		conn = WsConnection.create("robotoutgui25:"+port,"wsupdates");
        	}
        	else{
        		conn = WsConnection.create(ip+":"+port,"wsupdates");
        	}
	        CommUtils.outcyan("OutInWsGuimap | OutInWsGuimap connected to " + port);	        
	        ((WsConnection) conn).addObserver(this); 		
//        } catch (Exception e) {
//        	CommUtils.outred("OutInWsGuimap | connectToRobotMind | ERROR:" +e.getMessage());
//        	connectToRobotMind(  port );
//        }    	   	
    }
    
    public void send(String msg) {
        try {
        	//CommUtils.outyellow("		OutInWsGuimap send " + msg );	   
        	if( conn != null ) conn.forward(msg);   
        } catch (Exception e) {
        	CommUtils.outred("               OutInWsGuimap | ERROR:" +e.getMessage());
        }     	
    }

     
 
    
	@Override 
	public void update(Observable o, Object arg) {
		//CommUtils.outyellow("OutInWsGuimap | riceve da observale: " + o + " la info:" + arg);		
		update(arg.toString() );
	}


	@Override
	public void update(String message) {
		//CommUtils.outcyan("OutInWsGuimap | update elabora: " + message);
	}
 

//    public static void main(String[] args) {
//    	try {
//			OutInWsGuimap caller = new OutInWsGuimap("8085");
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//    	//caller.workWithGui(); 
//    }
    
    
    /*
     * Just to test ...
     */
    public static int ncellend   = 0;
    public static int ncellstart = 0;
    public static int ncellemit  = 0;
    public static void incend() {
    	ncellend++;
    	CommUtils.outblue("ncellend="+ncellend);
    }
    public static void incemit() {
    	ncellemit++;
    	//CommUtils.outblue("ncellemit="+ncellemit);
    }

    public static void incstart() {
    	ncellstart++;   	 
    }
 
} 
