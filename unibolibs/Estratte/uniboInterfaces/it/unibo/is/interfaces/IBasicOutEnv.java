package it.unibo.is.interfaces;

public interface IBasicOutEnv {
	/**
	*	Return the output virtual device.
	*/
	public IOutputView getOutputView();
	public IOutputEnvView getOutputEnvView();
	/**
	*	Print a string on the virtual output device
	*/
	public void println( String msg );	

}
