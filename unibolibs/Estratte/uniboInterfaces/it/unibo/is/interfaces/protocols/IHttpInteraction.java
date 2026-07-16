package it.unibo.is.interfaces.protocols;

public interface IHttpInteraction  extends IConnInteraction{
	public String sendRequest(String url) throws Exception;
	public String postRequest(String request) throws Exception;	
}
