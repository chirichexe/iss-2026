package it.unibo.is.interfaces.protocols;
import java.net.DatagramSocket;
 
public interface IUdpInteraction extends IConnInteraction{
	public DatagramSocket getSocket();
}
