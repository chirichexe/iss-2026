package it.unibo.contactEvent.interfaces;

import java.util.Hashtable;


public interface IEventItem {
	public String getEventId();
	public String getSubj();
	public ILocalTime getTime();
  	public String getMsg();
  	public String getPrologRep();
  	public String getDefaultRep();
  	public Hashtable<String,Object> getArgTable();
}
