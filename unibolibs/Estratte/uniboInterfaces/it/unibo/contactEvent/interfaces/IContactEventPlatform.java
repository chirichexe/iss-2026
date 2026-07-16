package it.unibo.contactEvent.interfaces;

import java.util.Hashtable;
 
 
  
public interface IContactEventPlatform {
//Activate a qevent component
	public void activate(String subj);
//Emits an event from the emitter
	public void raiseEvent( String emitter, String evId, String evContent ) ;
//	public void raiseEvent( String emitter, String evId, String evContent, Hashtable<String, Object> at ) ;
//Gets the platform local time (one tick for each event)
	public ILocalTime getLocalTime();
//Register a component for an event
	public void registerForEvent(String subj, String ev);
//Unregister a component for an event
	public void unregisterForEvent(String subj, String ev);
	public void unregisterForAllEvents( String subj );
//

}
