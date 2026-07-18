package smart;
import java.util.List;
import gui.AdapterGuiForMindrep;
import it.unibo.kactor.GuiColors;
import location.RoomMap;
import location.RoomMap.Direction;
import planning.Node;
import unibo.basicomm23.utils.CommUtils;

 
public class RobotPosUtils {
	/** Singleton instance of RobotPathUtils */
	private static RobotPosUtils instance;
	/** Current robot direction during path planning */
	private  Direction dir;
	/** Current robot direction during execution */
	private  Direction curdir;
	/** Current robot position */
	private Node curPos;
	/** GUI adapter for visualization */
	protected AdapterGuiForMindrep gui;
	
	protected static String guiIp;
	
	public static RobotPosUtils getInstance(  String ip ) {
		AdapterGuiForMindrep gui = AdapterGuiForMindrep.create(ip);
		guiIp = ip;
 		return getInstance( AdapterGuiForMindrep.create(ip) );
	}

	/**
	 * Gets or creates the singleton instance of RobotPathUtils
	 * 
	 * @param gui The GUI adapter for visualization
	 * @return The singleton RobotPathUtils instance
	 */
	public static RobotPosUtils getInstance(AdapterGuiForMindrep gui) {
		if( instance == null ) instance = new RobotPosUtils(gui);
		return instance;
	}
	//May2026 Called by AStarPathfinding che deve mostrare le ricerche del path
	public static RobotPosUtils getInstance(  ) {
		return instance;
	}
	
	/**
	 * Constructor initializes the robot path utilities
	 * 
	 * @param gui The GUI adapter for visualization
	 */
	public RobotPosUtils(AdapterGuiForMindrep gui) {	
//		CommUtils.outred("RobotPosUtils guiiiiiiiiiiiiiiiiiii = " + gui);	
		gui.showTheRoom();
		this.gui = gui;
	}
	
	public String getGuiIp() {
		return guiIp;
	}

    /**
     * DEFINIZIONE DEL PIANO COME SEQUENZA DI MOSSE
     * PLAN DEFINITION AS SEQUENCE OF MOVES
     */	

    /**
     * Generates movement commands for changing X coordinate (moving up/down)
     * Determines the optimal sequence of turns and forward movements to change
     * the robot's X position while maintaining proper direction
     * 
     * @param down True if moving down (increasing X), false if moving up (decreasing X)
     * @return String of movement commands (w, l, r combinations)
     */
    protected String changeX(boolean down) {
    	switch (dir) {
    		case DOWN  : if( down ) {return "w"; }
    		             else { dir =  Direction.UP; return "rrw"; }
    		case RIGHT : if( down ) { dir =  Direction.DOWN; return "rw"; }
    		             else { dir =  Direction.UP; return "lw";}
    		case LEFT  : if( down ) { dir =  Direction.DOWN; return "lw";}
    		             else { dir =  Direction.UP; return "rw"; }
    		case UP    : if( down ) { dir =  Direction.DOWN; return "rrw"; }
    		             else { dir =  Direction.UP; return "w"; }
    		default    : return "";
    	}
    }
    
    /**
     * Generates movement commands for changing Y coordinate (moving left/right)
     * Determines the optimal sequence of turns and forward movements to change
     * the robot's Y position while maintaining proper direction
     * 
     * @param right True if moving right (increasing Y), false if moving left (decreasing Y)
     * @return String of movement commands (w, l, r combinations)
     */
    protected String changeY(boolean right) {
    	switch (dir) {
    		case DOWN  : if( right ) { dir =  Direction.RIGHT; return "lw";} 
    		             else {  dir =  Direction.LEFT;  return "rw"; }
    		case RIGHT : if( right ) { return "w"; } 
    		             else { dir =  Direction.LEFT; return "rrw";  }
    		case LEFT  : if( right ) { dir =  Direction.RIGHT; return "rrw"; }
    					 else {  return "w"; }
    		case UP    : if( right ) {  dir =  Direction.RIGHT; return "rw"; }
    					 else {  dir = Direction.LEFT; return "lw"; }
    		default    : return "";
    	}
    }

    /**
     * Converts a path of nodes into a sequence of robot movement commands
     * Assumes robot starts facing DOWN direction and generates optimal movement
     * commands to follow the path from start to target
     * 
     * The method processes the path step by step:
     * 1. For each consecutive pair of nodes in the path
     * 2. Determines if movement is in X or Y direction
     * 3. Generates appropriate movement commands
     * 4. Updates robot direction accordingly
     * 
     * @param path List of nodes representing the path to follow
     * @param start Starting node (used for direction initialization)
     * @param target Target node (for reference)
     * @return String of movement commands (w, l, r combinations)
     */
    public String FromPathToMoves(List<Node> path, Node start, Node target) {
    	// Assumption: robot direction in start is down
    	StringBuilder moves = new StringBuilder("");
    	if( path.size() == 1) return moves.toString();
    	dir = RoomMap.Direction.DOWN;

    	/*
    	 * x sono le righe (rows)
    	 * y sono le colonne (columns)
    	 */
    	
        while( path.size() > 1 ) {
        	Node current = path.get(0);
            Node next    = path.get(1);    	
            
            // Determine movement direction and generate commands
            if( (next.x == current.x) ) { 
            	// Moving in Y direction (left/right)
            	moves.append(changeY(next.y > current.y));  
            }
            if( (next.y == current.y) ) { 
            	// Moving in X direction (up/down)
            	moves.append(changeX(next.x > current.x));  
            }
            
            path.remove(0); // Remove processed node           	 
        }  
    	return moves.toString();
    }

    /**
     * ESECUZIONE DEL PIANO COME SEQUENZA DI MOSSE
     * PLAN EXECUTION AS SEQUENCE OF MOVES
     */
    
    /**
     * Executes a right turn and updates robot direction and GUI display
     * Changes the robot's direction clockwise and updates the visual representation
     */
    public void turnRight() {
    	switch (curdir) {	
			case DOWN  : { curdir =  Direction.LEFT;   gui.displayOnGui(curPos.y, curPos.x, GuiColors.WEST); break; }
			case RIGHT : { curdir =  Direction.DOWN; gui.displayOnGui(curPos.y, curPos.x, GuiColors.SUD); break; }
			case LEFT  : { curdir =  Direction.UP; gui.displayOnGui(curPos.y, curPos.x, GuiColors.NORD); break;}
			case UP    : { curdir =  Direction.RIGHT; gui.displayOnGui(curPos.y, curPos.x, GuiColors.EST); break;}
    	}
    }
    
    /**
     * Executes a left turn and updates robot direction and GUI display
     * Changes the robot's direction counter-clockwise and updates the visual representation
     */
    public void turnLeft() {
    	switch(curdir) {	
			case DOWN  : {curdir =  Direction.RIGHT;  gui.displayOnGui(curPos.y, curPos.x, GuiColors.EST); break;}
			case RIGHT : {curdir =  Direction.UP; gui.displayOnGui(curPos.y, curPos.x, GuiColors.NORD);  break;}
			case LEFT  : {curdir =  Direction.DOWN; gui.displayOnGui(curPos.y, curPos.x, GuiColors.SUD); break;}
			case UP    : {curdir =  Direction.LEFT; gui.displayOnGui(curPos.y, curPos.x, GuiColors.WEST); break;}
    	}
    }

    public void turnDown() {
    	switch(curdir) {	
			case DOWN  : { gui.displayOnGui(curPos.y, curPos.x, GuiColors.SUD); break;}
			case RIGHT : { turnRight(); break; }
			case LEFT  : { turnLeft(); break;}
			case UP    : { turnRight(); turnRight(); break;}
		//default    :  ;
    	}
    }

    public void turn(String dir) {
    	if( dir.equals("a") || dir.equals("l")) turnLeft();
    	else if( dir.equals("d") || dir.equals("r") ) turnRight();
    }

    public void forwardStep() {
    	//CommUtils.outyellow("RobotPosUtil | forwarStep curdir=" + curdir + " curPos=" + curPos);
    	if( curPos.x == 0 && curPos.y == 0 ){
    		//CommUtils.outred("RobotPosUtil | at home ");
    		if( curdir == curdir.LEFT ||  curdir == curdir.UP ) return;
    	}
    	gui.displayOnGui(curPos.y,curPos.x,GuiColors.ACQUA);
    	switch(curdir) {	
			case DOWN  : { curPos= new Node( curPos.x+1,curPos.y );
			               gui.displayOnGui(curPos.y, curPos.x, GuiColors.SUD);
			               break;
			             }
			case RIGHT : { curPos= new Node( curPos.x,curPos.y+1 );
						   gui.displayOnGui(curPos.y, curPos.x, GuiColors.EST);
						   break;
						 } 
			case LEFT  : { curPos= new Node( curPos.x,curPos.y-1 );
			               gui.displayOnGui(curPos.y, curPos.x, GuiColors.WEST);
			               break;
			             }
			case UP    : { curPos= new Node( curPos.x-1,curPos.y );
				            gui.displayOnGui(curPos.y, curPos.x, GuiColors.NORD);
				            break;
				          }
		//default    :  ;
    	}
    	//CommUtils.outyellow("RobotPosUtil | forwarStep curdir=" + curdir + " curPos=" + curPos);
    }
 
    public String planToSetDirection(String dir){
        String plan ="";
        String direction = getCurDir();
        if( direction.equals("up") ) {
            switch (dir) {
                case "up":
                    break;
                case "left":
                    plan="l";
                    break;
                case "right":
                    plan="r";
                    break;
                case "down":
                    plan="ll";
                    break;
            }
        }
        else if( direction.equals("down") ){
            switch (dir) {
                case "up":
                    plan="ll";
                    break;
                case "left":
                    plan="r";
                    break;
                case "right":
                    plan="l";;
                    break;
                case "down":
                    break;
            }
        }
        else if( direction.equals("right") ){
            switch (dir) {
                case "up":
                    plan="l";
                    break;
                case "left":
                    plan="ll";
                    break;
                case "right":
                    break;
                case "down":
                    plan="r";
                    break;
            }
        }
        else if( direction.equals("left") ){
            switch (dir) {
                case "up":
                    plan="r";
                    break;
                case "left":
                    break;
                case "right":
                    plan="ll";
                    break;
                case "down":
                    plan="l";
                    break;
            }
        }
        CommUtils.outmagenta("RobotPathUtils | setTheDirection " + dir + " while " + direction + "=" +plan);
        return plan;
    }

   public void setCurPos(int x, int y) {
	   curPos.x = x;
	   curPos.y = y;
   }
   public int getPosX() {
	   return curPos.x;
   }
   
   public int getPosY() {
	   return curPos.y;
   }    
   
   public String getMapRep() {
	   return gui.getMapRep();
   }
   
   public void setCurdir(RoomMap.Direction dir) {
   	curdir =dir;
   }
   public String getCurDir( ) {
	   switch(curdir) {
		   case DOWN  : return "down";  
		   case UP    : return "up"; 
		   case LEFT  : return "left"; 
		   case RIGHT : return "right";
		   default    : return null;
	   }
   }
   
   public void setRobotState(int X, int Y, String DIR) {
	   setCurPos(X,Y);
	   switch(DIR) {
		   case "down"  : { curdir = Direction.DOWN;  break; }
		   case "up"    : { curdir = Direction.UP;  break;   }
		   case "left"  : { curdir = Direction.LEFT;  break;	 }
		   case "right" : { curdir = Direction.RIGHT;  break;	 }	    
	   }
	   gui.showTheRoom();
	   showTheRobotState();
	   
   }
    public void doMove(String move) {
    	//CommUtils.outblack("RobotPathUtils | doMove=" + move);
    	switch(move) {
	    	case "r","d" : {turnRight();  break; }
	    	case "l","a" : {turnLeft();   break; }
	    	case "w" :     {forwardStep(); break; }
	    	default    :  CommUtils.outred("RobotPathUtils | doMove TODO " + move);
        }
    }
    

    public void robotAtHome( ) {
    	curPos = new Node(0,0);
    	curdir = RoomMap.Direction.DOWN;
    	gui.displayOnGui(0,0,GuiColors.SUD);
    }
    
    public void robotSetPos( String VX, String VY, String dir) {
    	int X = Integer.parseInt(VX);
    	int Y = Integer.parseInt(VY);
    	curPos = new Node(X,Y);
    	//dir = up|down!left|right
    	switch( dir ) {
	    	case "down","downDir": 	curdir = RoomMap.Direction.DOWN;  gui.displayOnGui(X,Y,GuiColors.SUD);break;
	    	case "left", "leftDir": curdir = RoomMap.Direction.LEFT;  gui.displayOnGui(X,Y,GuiColors.WEST);break;
	    	case "up","upDir":		curdir = RoomMap.Direction.UP;    gui.displayOnGui(X,Y,GuiColors.NORD);break;
	    	case "right","rightdir":curdir = RoomMap.Direction.RIGHT; gui.displayOnGui(X,Y,GuiColors.EST);break;
    	}
    	CommUtils.outred("RobotPathUtils  robotSetPos X=" + X + " Y=" + Y + " D=" + dir + " curPos=" + curPos + " curdir=" + curdir) ;
    }   
    
    /*
     * Piano a livello mind 
     */
    
    public void doPlan(String moves, Node start, Node target) {
    	CommUtils.outmagenta("start="+start + " target=" + target);
    	CommUtils.outmagenta(""+moves);
    	curPos = start;
    	gui.displayOnGui(start.y,start.x,GuiColors.SUD);
     	
    	gui.displayOnGui(target.y, target.x, GuiColors.TARGET);  //mapstyle.css (.target) iomap.js (13)
    	
    	String move = ""+moves.charAt(0);
    	//CommUtils.delay(1000); //(A)
    	doMove( move ); 
    	moves = moves.substring(1);
    	while( moves.length() > 0 ) {
    		//CommUtils.delay(1000); //(A)
    		move = ""+moves.charAt(0);
    		
    		doMove( move );
    		moves = moves.substring(1);
    	}
    	//CommUtils.delay(1000);   	 //(A)
//    	turnDown();
 
    	
    }
    
    public void showTheRobotState() {
    	switch (curdir) {	
			case DOWN  : {  gui.displayOnGui(curPos.y, curPos.x, GuiColors.SUD); break; }
			case RIGHT : {  gui.displayOnGui(curPos.y, curPos.x, GuiColors.EST); break; }
			case LEFT  : {  gui.displayOnGui(curPos.y, curPos.x, GuiColors.WEST); break;}
			case UP    : {  gui.displayOnGui(curPos.y, curPos.x, GuiColors.NORD); break;}
    	}
     }
	
}
