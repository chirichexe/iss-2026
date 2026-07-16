package it.unibo.contactEvent.interfaces;
public interface IActorMessage  {
	public String msgId();	
	public String msgType();	
	public String msgSender();	
	public String msgReceiver();	
	public String msgContent();	
	public String msgNum();
	public String getDefaultRep();
}