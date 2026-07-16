package it.unibo.is.interfaces;
import java.awt.Component;
import java.awt.Panel;

public interface IBasicEnvAwt extends IBasicUniboEnv{
 	public void initNoFrame();
	public IOutputEnvView getOutputEnvView();
	/**
	 *	Write on the status bar
	 */
	public void writeOnStatusBar( String s, int size);
	/**
	 * @return true in case of a standalone application 
	 */
	public boolean isStandAlone();	
	/**
	*	Add a panel in the environment.
	*/
	public void addInputPanel( int size );
	public void addInputPanel( String msg );
	public void addPanel( Panel  p );
	public void addPanel( Component  p );
	/**
	*	Builds a command panel (of class CmdPanel) and adds it to the environment.
	*/
	public Panel addCmdPanel(String name, String[] commands, IActivity activity);
	public Panel addCmdPanel(String name, String[] commands, IActivityBase activity);
	/**
	*	Remove a panel form the environment.
	*/
	public void removePanel(  Panel p);
  	/**
	 * @return the numer of panels
	 */
	public int getNumOfPanels();
	/**
	*	Makes the GUI environment visibile.
	*/
	public void setEnvVisible( boolean b );	
  }