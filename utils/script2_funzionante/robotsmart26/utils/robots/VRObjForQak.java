package robots;

import it.unibo.kactor.ActorBasic;
import it.unibo.kactor.MsgUtil;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.utils.CommUtils;

public class VRObjForQak extends VRObj26{
private ActorBasic owner;

	public VRObjForQak( ActorBasic owner, String logFName ) {
		super( null,logFName );
		this.owner = owner;
		CommUtils.outmagenta("     VRObjForQak  | created "  );
 	}

    protected void emitInfo(IApplMessage info) {
    	if( owner != null) {
    		//CommUtils.aboutThreads("RobotObjForQak  | emitinfo");
     	    if( tracing) CommUtils.outyellow("     RobotObjForQak  | emitLocalStreamEvent " + info.msgContent() );
    		if( info.msgContent().contains("collision")) return;
    		//MsgUtil.emitLocalEvent(info,owner,null); 
    		MsgUtil.emitLocalStreamEvent(info, owner, null); 
      	}else {
    		CommUtils.outmagenta("     RobotObjForQak  | emitInfo " + info );
     	}
     }

}
