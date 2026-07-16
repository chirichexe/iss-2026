package it.unibo.iot.interfaces;
/*
 * -----------------------------------------------------------
 * This is the basic model of a (IOT) Device:
 * an entity with a name and 
 * a default representation (expressed in Prolog syntax)
 * -----------------------------------------------------------
 */
public interface IDeviceIot {
	public String getName(); 	    //property	
	public String getDefaultRep(); 	//mapping
}