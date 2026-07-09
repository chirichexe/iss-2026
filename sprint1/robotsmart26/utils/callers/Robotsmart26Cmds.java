package callers;

import alice.tuprolog.Struct;
import alice.tuprolog.Term;
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.utils.CommUtils;

/* 
 * ---------------------------------------------
 * Robotsnart26Cmds - Robot Command Interface
 * ---------------------------------------------
 * This class provides a collection of predefined robot commands and movement
 * operations for the basic robot system. It contains static message definitions
 * for various robot actions like turning, moving, and state management.
 * 
 * The class serves as a command library for robot operations, allowing
 * easy access to common robot movements and configurations.
 */

public class Robotsmart26Cmds {
	
	/**
	 * Default caller identifier for robot commands
	 * Used as the sender ID in all robot communication messages
	 */
	public static String callerName = "acaller";

 
    // ===========================================
    // BASIC MOVEMENT COMMANDS
    // ===========================================
    
    /** Command to turn the robot left */
    public static IApplMessage turnL = CommUtils.buildDispatch(callerName, "move", "move(l)", "robotsmart");
    
    /** Command to turn the robot right */
    public static IApplMessage turnR = CommUtils.buildDispatch(callerName, "move", "move(r)", "robotsmart");
    
    /** Command to execute a predefined movement plan */
    public static IApplMessage doplan = CommUtils.buildRequest(callerName, "doplan", "doplan(PLAN,330)", "robotsmart");
    //public static IApplMessage doplan = CommUtils.buildRequest(callerName, "doplan", "doplan(lwwwwlwrwlwwlw,330)", "robotsmart");
    
    // ===========================================
    // POSITION-BASED MOVEMENT COMMANDS
    // ===========================================
    
    /** Move robot to port position (4,0) with speed 335 */
    public static IApplMessage moveInPort = CommUtils.buildRequest(callerName, "moverobot", "moverobot(4,0,335)", "robotsmart");
    
    /** Move robot to position (1,1) with speed 335 */
    public static IApplMessage move11 = CommUtils.buildRequest(callerName, "moverobot", "moverobot(1,1,335)", "robotsmart");
    
    /** Move robot to position (1,4) with speed 335 */
    public static IApplMessage move14 = CommUtils.buildRequest(callerName, "moverobot", "moverobot(1,4,335)", "robotsmart");
    
    /** Move robot to position (3,2) with speed 335 */
    public static IApplMessage move32 = CommUtils.buildRequest(callerName, "moverobot", "moverobot(3,2,335)", "robotsmart");
    
    /** Move robot to position (5,3) with speed 335 */
    public static IApplMessage move53 = CommUtils.buildRequest(callerName, "moverobot", "moverobot(5,3,335)", "robotsmart");
    
    /** Move robot to position (4,0) with speed 335 */
    public static IApplMessage move40 = CommUtils.buildRequest(callerName, "moverobot", "moverobot(4,0,335)", "robotsmart");
    
    /** Move robot to position (4,3) with speed 335 */
    public static IApplMessage move43 = CommUtils.buildRequest(callerName, "moverobot", "moverobot(4,3,335)", "robotsmart");
    
    /** Move robot to position (3,4) with speed 335 */
    public static IApplMessage move34 = CommUtils.buildRequest(callerName, "moverobot", "moverobot(3,4,335)", "robotsmart");
    
    /** Move robot to home position (0,0) with speed 335 */
    public static IApplMessage moveHome = CommUtils.buildRequest(callerName, "moverobot", "moverobot(0,0,335)", "robotsmart");
    
    // ===========================================
    // ROBOT STATE AND CONFIGURATION COMMANDS
    // ===========================================
    
    /** Set robot position to home (0,0) facing down */
    public static IApplMessage setHome  = CommUtils.buildDispatch(callerName, "setrobotstate", "setpos(0,0,down)", "robotsmart");
    
    /** Set robot direction to up */
    public static IApplMessage setDirectionUp    = CommUtils.buildDispatch(callerName, "setdirection", "dir(up)", "robotsmart");
    
    /** Set robot direction to down */
    public static IApplMessage setDirectionDown  = CommUtils.buildDispatch(callerName, "setdirection", "dir(down)", "robotsmart");
    
    /** Request current robot state information */
    public static IApplMessage robotstate = CommUtils.buildRequest(callerName, "getrobotstate", "getrobotstate(now)", "robotsmart");
     
    // ===========================================
    // PLAN BUILDING DELAY CONFIGURATION
    // ===========================================
    
    /** Reset plan build delay to 0 (fast planning) */
    public static IApplMessage resesetplanbuildelay  = CommUtils.buildDispatch(callerName, "setplanbuildelay", "setplanbuildelay(0)", "robotsmart");
    
    /** Restore plan build delay to 80 (normal planning speed) */
    public static IApplMessage restoreplanbuildelay  = CommUtils.buildDispatch(callerName, "setplanbuildelay", "setplanbuildelay(80)", "robotsmart");
     
 
    /** Request to build a path plan from (4,0) to (1,4) */
    public static IApplMessage buildPlan = CommUtils.buildRequest(callerName, "buildPlan", "buildPlan(4,0,1,4)", "robotsmart");

    /** Alarm event message for emergency situations */
    public static IApplMessage alarm = CommUtils.buildEvent(callerName, "alarm", "alarm(fromcaller)");
 
    // ===========================================
    // DIRECTIONAL COMMANDS (ALTERNATIVE FORMAT)
    // ===========================================
    
     
    /** Command to turn left (l) */
    public static IApplMessage cmdl = CommUtils.buildDispatch(callerName, "move", "move(l)", "robotsmart");
    
    /** Command to turn right (r) */
    public static IApplMessage cmdr = CommUtils.buildDispatch(callerName, "move", "move(r)", "robotsmart");

    /** Request to tune robot at home position */
    public static IApplMessage tuneathome = CommUtils.buildRequest(callerName, "tuneAtHome", "tuneAtHome(ok)", "robotsmart");


	/**
	 * Checks if a robot movement operation failed and handles the failure response
	 * Parses the failure message to extract information about completed and remaining moves
	 * 
	 * @param answer The response message from the robot
	 * @param connSupport The connection support for robot communication (can be null)
	 * @throws Exception If there's an error processing the response
	 */
	protected void checkPlanexecAnswer( IApplMessage answer, Interaction connSupport ) throws Exception {
		if( answer.msgId().equals("moverobotfailed")) {
			//CommUtils.outred("moveTarget answer=" + answer.msgContent());
			Struct msgStruct = (Struct) Term.createTerm(answer.msgContent());
			String movesdone = msgStruct.getArg(0).toString();
			String movestodo = msgStruct.getArg(1).toString();
			CommUtils.outred("moveTarget answer=" + answer.msgContent() + " done=" + movesdone + " todo=" + movestodo);
		}
 
	}
    

}
