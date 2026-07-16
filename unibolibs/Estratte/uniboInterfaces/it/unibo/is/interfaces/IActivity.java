package it.unibo.is.interfaces;

public interface IActivity extends IActivityBase{
	public  void execAction( );
 	public  void execAction(IIntent input);
	public String execActionWithAnswer(String cmd);
}
