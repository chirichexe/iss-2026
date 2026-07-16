package it.unibo.is.interfaces.protocols;
import java.net.DatagramSocket;
 
public interface IUdpConnection {	
	public  DatagramSocket connectAsReceiver(int portNum) throws Exception;
	public DatagramSocket connectAsClient(String hostName, int port) throws Exception;
	public void closeConnection(DatagramSocket socket);

}
