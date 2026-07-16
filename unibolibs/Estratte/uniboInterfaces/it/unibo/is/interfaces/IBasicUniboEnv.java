package it.unibo.is.interfaces;

public interface IBasicUniboEnv {
	public void init();
	public  String readln(  );	
	public IOutputView getOutputView();
	public void println( String msg );	
	public void close(   );
 }