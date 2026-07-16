package unibo.basicomm23.enablers;

import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ProtocolType;
import unibo.basicomm23.utils.CommUtilsOrig;
import unibo.basicomm23.utils.ConnectionFactory;

public class CallerAsClient {
private Interaction conn;
protected String name ;


	public CallerAsClient(String name, String host, String entry, ProtocolType protocol ) {
		try {
			CommUtilsOrig.outblue(name+"  | CREATING entry= "+entry+" protocol=" + protocol );
			this.name  = name;
			conn       = ConnectionFactory.createClientSupport23(protocol,host,entry);
			CommUtilsOrig.outblue(name+"  | CREATED entry= "+entry+" conn=" + conn );
		} catch (Exception e) {
			CommUtilsOrig.outred( name+"  |  ERROR " + e.getMessage());		}
	}
	public Interaction getConn() {
		return conn;
	}

}
