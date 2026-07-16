package it.unibo.contactEvent.interfaces;

public interface IContactComponent  extends Runnable{ 
	public void doJob() throws Exception;
	public void terminate() throws Exception;
	public boolean isPassive();
	public boolean isTerminated();
	public boolean isActivated();
	public  void resume(IEventItem ev);
	public String getName();
}
