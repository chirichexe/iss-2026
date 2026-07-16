package it.unibo.is.interfaces;

import it.unibo.is.interfaces.platforms.IRawBuffer;

public interface IContentSegmentable {
//SENDING part	
 	public int getSize() throws Exception;
	public byte[] nextSegment() throws Exception;
	public void closeSending() throws Exception;
//RECEIVING PART
	public void newSegment(IRawBuffer segmentRaw) throws Exception;
	public void closeReceiving() throws Exception;
	
}
