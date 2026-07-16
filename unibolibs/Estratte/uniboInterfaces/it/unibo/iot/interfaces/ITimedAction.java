package it.unibo.iot.interfaces;

public interface ITimedAction extends IAction{
	public long getExecTime();	 
	public String getTerminationEventId(); 
}
