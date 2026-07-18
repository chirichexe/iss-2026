package gui;

import it.unibo.kactor.GuiColors;
import planning.Node;
import unibo.basicomm23.utils.CommUtils;
 
/**
 * AdapterGuiForMindrep - GUI Adapter for Robot Visualization
 * 
 * This class provides a bridge between the robot control system and the graphical user interface.
 * It handles the display of the robot's environment, position, and movement paths on a grid-based GUI.
 * The class uses a singleton pattern to ensure only one instance manages the GUI display.
 * 
 * Key responsibilities:
 * - Loading and managing the environment map from file
 * - Displaying the robot's current position and direction
 * - Visualizing planned paths and obstacles
 * - Converting grid coordinates to GUI display commands
 * 
 * @author Unibo BasicRobot25 Team
 * @version 2025
 */
public class AdapterGuiForMindrep {
	/** Singleton instance of AdapterGuiForMindrep */
	private static AdapterGuiForMindrep instance = null;
	
	/** Utility for map operations and grid management */
	protected MapUtil maputil = new MapUtil();

	/**
	 * Grid representation of the environment
	 * 0 = traversable space, 1 = obstacle
	 * Grid is loaded from map file and represents the robot's environment
	 */
	public static int[][] grid;
	
	/** Number of rows in the grid */
	protected int NR;
	/** Number of columns in the grid */
	protected int NC;		
	
	/** WebSocket connection for GUI communication on port 8085 */
	protected OutInWsGuimap outinws ;
	
	/**
	 * Creates or returns the singleton instance of AdapterGuiForMindrep
	 * 
	 * @return The singleton AdapterGuiForMindrep instance
	 */
	public static AdapterGuiForMindrep create(String ip) {
		if( instance == null ) { 
			instance = new AdapterGuiForMindrep(ip);
		}
		return instance;
	}
	public static AdapterGuiForMindrep getInstance( ) {
		if( instance == null ) { 
			CommUtils.outred("AdapterGuiForMindrep NOT YET CREATED");
			return null;
		}
		else return instance;
	}
	
	/**
	 * Constructor initializes the grid from map file and sets up dimensions
	 * Loads the environment map from "tf25map.txt" and initializes grid dimensions
	 */
	public AdapterGuiForMindrep(String ip) {
		try {
			grid = maputil.createGridFromMapInFile("tf25map.txt");
		    NR = grid.length;
		    NC = grid[0].length;
		    CommUtils.outyellow("AdapterGuiForMindrep | map " + NR + " x " + NC);
		    //La robotoutgui25 sulla 8085 potrebbe non essere attiva:
		    //La mente esiste ma non ha una rappresentazione
			outinws = OutInWsGuimap.getResource(ip,"8085");
		} catch (Exception e) {
			CommUtils.outred("AdapterGuiForMindrep | WARNING: working without map gui");
 		}
		
	}

	/**
	 * Displays a cell on the GUI with specified coordinates and color
	 * Sends a formatted message to the GUI via WebSocket connection
	 * 
	 * @param X The X coordinate (column) in the grid
	 * @param Y The Y coordinate (row) in the grid  
	 * @param c The color to display the cell in
	 */
    public void displayOnGui( int X, int Y, GuiColors c) {
    	// Format message for GUI: "cell(X,Y,Z)" where Z is color ordinal
    	String msg = "cell(X,Y,Z)".replace("X",""+X).replace("Y",""+Y).replace("Z",""+c.ordinal());
    	if( outinws != null) outinws.send(msg);
    }

    /**
     * Displays the entire room/environment on the GUI
     * Iterates through the grid and displays each cell:
     * - Black for obstacles (grid value = 1)
     * - Aqua for traversable space (grid value = 0)
     */
    public void showTheRoom() {
        for (int r = 0; r < NR; r++) {
            for (int c = 0; c < NC; c++) {
                if (grid[r][c] == 1) {	
                    // Display obstacle in black
                    displayOnGui(c,r,GuiColors.BLACK);
                } else {
                    // Display traversable space in aqua
                    displayOnGui(c,r,GuiColors.ACQUA);
                }
             }
        }  	
    }
    
    /**
     * Displays the current path from start to target node
     * Shows the room first, then overlays the path with different colors:
     * - Yellow for target position
     * - Magenta for current position  
     * - Blue for path nodes
     * 
     * @param current The current node position
     * @param target The target node position
     */
    public void showCurrentPath(Node current, Node target) {
    	showTheRoom();  // Display base room first to avoid path overlap
    	displayOnGui(target.y,target.x,GuiColors.YELLOW); // Target in yellow
    	if( current != null ) {
    		displayOnGui(current.y,current.x,GuiColors.MAGENTA); // Current position in magenta
    		current = current.parent;
    	}
    	// Display path from current to start in blue
    	while (current != null) {
    		 displayOnGui(current.y,current.x,GuiColors.BLUE);
    		 current = current.parent;
    	}
    }
    
    /**
     * Converts the grid representation to a string format
     * Creates a single-line string representation of the grid where:
     * - Each row is separated by "|"
     * - Each cell contains its value (0 or 1)
     * 
     * @return String representation of the grid map
     */
    public String getMapRep() {
    	String s = "";
    	for( int i=0; i<NR; i++ ) {
    		for( int j=0; j<NC; j++ ) {
     			s = s + grid[i][j];
     		}
    		s = s+"|"; // Row separator
    	}
    	return s;
    }
 
}//AdapterGuiForMindrep
