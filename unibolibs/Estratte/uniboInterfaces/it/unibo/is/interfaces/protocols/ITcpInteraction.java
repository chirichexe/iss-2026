package it.unibo.is.interfaces.protocols;

import java.net.Socket;

public interface ITcpInteraction extends IConnInteraction{
	public Socket getSocket();
}
