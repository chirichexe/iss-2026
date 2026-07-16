package unibo.basicomm23.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import unibo.basicomm23.enablers.ConnectionHandler;
import unibo.basicomm23.interfaces.IApplMsgHandler;
import unibo.basicomm23.utils.CommUtilsOrig;


public class UdpServer extends Thread{
private DatagramSocket socket;
private byte[] buf;
public Map<UdpEndpoint,UdpServerConnection> connectionsMap; //map a port to a specific connection object, if any
protected IApplMsgHandler userDefHandler;
protected String name;
protected boolean stopped = true;

 	public UdpServer( String name, int port,  IApplMsgHandler userDefHandler   ) {
		super(name);
		connectionsMap = new ConcurrentHashMap<UdpEndpoint,UdpServerConnection>();
	      try {
	  		this.userDefHandler   = userDefHandler;
	  		CommUtilsOrig.outblue(getName() + " | costructor port=" + port );
			this.name             = getName();
			socket                = new DatagramSocket( port );
	     }catch (Exception e) {
			  CommUtilsOrig.outred(getName() + " | costruct ERROR: " + e.getMessage());
	     }
	}
	
	@Override
	public void run() {
	      try {
	      	CommUtilsOrig.outblue( "UdpServer | STARTING ... "  );
			while( ! stopped ) {
				//Wait a packet				 
				//CommUtils.outblue( "UdpServer | waits a packet "   );
				buf = new byte[UdpConnection.MAX_PACKET_LEN];
				DatagramPacket packet = new DatagramPacket(buf, buf.length);
				socket.receive(packet);
				InetAddress address = packet.getAddress();
	            int port = packet.getPort();
	            UdpEndpoint client = new UdpEndpoint(address, port);
	            //String received = new String(packet.getData(), 0, packet.getLength());
				CommUtilsOrig.outyellow( "UdpServer | received packet from " + client    );
	            UdpServerConnection conn = connectionsMap.get(client);
	            if( conn == null ) {
	            	conn = new UdpServerConnection(socket, client, connectionsMap);
					CommUtilsOrig.outmagenta("UdpServer | CONNECTION  SET conn= " + conn + " client="+ client   );
	            	connectionsMap.put(client, conn);
			 		//Create HERE a message handler on the connection !!!
			 		//new UdpApplMessageHandler( userDefHandler, conn );
					new ConnectionHandler("UdpMsgHandler", userDefHandler, conn);
            	}else {
					CommUtilsOrig.outyellow("UdpServer | CONNECTION ALREADY SET conn= " + conn + " client="+ client   );
	            }
	            conn.handle(packet);		 
			}//while
		  }catch (Exception e) {  //Scatta quando la deactive esegue: serversock.close();
			  CommUtilsOrig.outred( "UdpServer |  probably socket closed: " + e.getMessage() );
		  }
	}
	
	public void activate() {
		if( stopped ) {
			stopped = false;
			this.start();
		}//else already activated
	}
 
	public void deactivate() {
		try {
			CommUtilsOrig.outblue( "UdpServer |  DEACTIVATE serversock=" +  socket );
			stopped = true;
			socket.close();
			connectionsMap.clear();
		} catch (Exception e) {
			CommUtilsOrig.outred( "UdpServer |  deactivate ERROR: " + e.getMessage());
		}
	}

	public int getNumConnections() {
		return connectionsMap.size();
	}
 
}
