package unibo.basicomm23.interfaces;

public interface InteractionBasic {
	public void forward(  IApplMessage msg ) throws Exception;
	public IApplMessage request(  IApplMessage msg ) throws Exception;
}
