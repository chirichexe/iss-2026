package it.unibo.is.interfaces;

public interface IAlarm {
	public void setTime(int time, String clientName);	
	public boolean timeOutExpired();
}
