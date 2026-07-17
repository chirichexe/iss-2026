package planning;
import java.io.FileReader;
import java.util.*;
import unibo.basicomm23.utils.CommUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import gui.AdapterGuiForMindrep;
import smart.RobotPosUtils;

/**
 * AStarPathfinding - A* Algorithm Implementation for Robot Pathfinding
 * 
 * This class implements the A* pathfinding algorithm to find optimal paths for robot navigation
 * in a grid-based environment. The algorithm finds the shortest path from a start position to
 * a target position while avoiding obstacles.
 * 
 * Key features:
 * - Manhattan distance heuristic for grid-based movement
 * - Configurable planning delays for realistic robot behavior
 * - Integration with GUI for path visualization
 * - JSON-based parameter configuration
 * 
 * The algorithm works by:
 * 1. Maintaining open and closed sets of nodes
 * 2. Using a priority queue to explore most promising paths first
 * 3. Calculating costs using gCost (actual) + hCost (heuristic)
 * 4. Reconstructing the optimal path once target is reached
 * 
 * Note: This algorithm finds the path but doesn't generate robot movement commands.
 * The actual robot movement sequence is determined by RobotPathUtils.FromPathToMoves()
 * which converts the path into specific robot commands (forward, turn left/right).
 * 
 * @author Unibo BasicRobot25 Team
 * @version 2025
 */
public class AStarPathfinding {

	/** Grid representation of the environment (shared with AdapterGui) */
	protected int[][] grid = AdapterGuiForMindrep.grid;
    /** Number of rows in the grid */
    protected int NR = grid.length;
    /** Number of columns in the grid */
    protected int NC = grid[0].length;
    
    /** Starting node for pathfinding */
    protected Node start;
    /** Target node for pathfinding */
    protected Node target;    
    /** Delay for plan building phase (milliseconds) */
    protected int PlanDBuildDelay = 30;
    /** Delay when path is found (milliseconds) */
    protected int FoundPathDelay = 1000;
    
    /** GUI adapter for visualization */
    protected AdapterGuiForMindrep gui; // = AdapterGui.create(); 
    /** Robot path utilities for movement conversion */
    protected RobotPosUtils robotposutil;  
    
    /**
     * Default constructor - reads planner parameters from JSON configuration
     */
    public AStarPathfinding( ) {
    	CommUtils.outcyan("AStarPathfinding -------AdapterGuiForMindrep should be alreday created");
    	gui = AdapterGuiForMindrep.getInstance(); 
     	//robot = RobotPosUtils.getInstance(gui);    //SENZA gui: 
     	robotposutil = RobotPosUtils.getInstance( );    //SENZA gui: TODO
    	readPlannerParams();
    }
    
    /**
     * Gets the map representation as a single-line string
     * Used for communication with other components
     * 
     * @return String representation of the grid map
     */
    public String getMapOneLine(){
        String map = "'"+gui.getMapRep()+"'";
        return map;
    }

    /**
     * Sets the plan building delay parameter
     * Controls timing of the planning phase
     * 
     * @param PlanBuildDelay The delay in milliseconds
     */
    public void setPlanBuildDelay(int PlanBuildDelay) {
    	this.PlanDBuildDelay = PlanBuildDelay;
    	CommUtils.outblue("AStarPathfinding setPlanBuildDelay="+this.PlanDBuildDelay);
    }
    
    /**
     * Constructor with start and target nodes
     * 
     * @param start The starting node for pathfinding
     * @param target The target node for pathfinding
     */
    public AStarPathfinding(Node start, Node target) {
    	this.start = start;
    	this.target = target;
    }
    
    /**
     * Reads planner parameters from JSON configuration file
     * Loads PlanDBuildDelay and FoundPathDelay from basicrobotParams.json
     * Uses default values if file is not found or parameters are missing
     */
    protected void readPlannerParams() {
    	CommUtils.outcyan("readPlannerParams ------------------------------------------------");
    	JSONParser parser = new JSONParser();
    	try (FileReader reader = new FileReader("basicrobotParams.json")) {
    		 Object obj = parser.parse(reader);
    		 if (obj instanceof JSONObject) {
    			 JSONObject jsonObject = (JSONObject) obj;
    			 // Read PlanDBuildDelay parameter
    			 Object value = jsonObject.get("PlanDBuildDelay");
                 if (value != null) {
                	 PlanDBuildDelay = Integer.parseInt(value.toString());
                 } 
    			 // Read FoundPathDelay parameter
    			 Object value1 = jsonObject.get("FoundPathDelay");
                 if (value1 != null) {
                	 FoundPathDelay = Integer.parseInt(value1.toString());
                 } 
             } 
    	} catch (Exception e) {
    		CommUtils.outred("readRobotbasicParms ERROR"+e.getMessage());
    	}
    }
    
    /**
     * Sets the start and target nodes for pathfinding
     * 
     * @param start The starting node
     * @param target The target node
     */
    public void setEndNodes(Node start, Node target) {
     	this.start = start;
    	this.target = target;    	
    }
    
    /**
     * Calculates heuristic cost using Manhattan distance
     * Manhattan distance is optimal for grid-based movement (up, down, left, right)
     * For diagonal movement, Euclidean distance would be more appropriate
     * 
     * @param node The current node
     * @param targetNode The target node
     * @return Manhattan distance between the nodes
     */
    private double calculateHeuristic(Node node, Node targetNode) {
        return Math.abs(node.x - targetNode.x) + Math.abs(node.y - targetNode.y);
        // For diagonal movement, you might use Euclidean distance:
        // return Math.sqrt(Math.pow(node.x - targetNode.x, 2) + Math.pow(node.y - targetNode.y, 2));
    }

    /**
     * Implements the A* pathfinding algorithm
     * Finds the optimal path from startNode to targetNode while avoiding obstacles
     * 
     * Algorithm steps:
     * 1. Initialize open set with start node
     * 2. While open set is not empty:
     *    - Get node with lowest fCost
     *    - If it's the target, reconstruct and return path
     *    - Add to closed set and explore neighbors
     *    - Update costs for better paths found
     * 3. Return null if no path exists
     * 
     * @param startNode The starting position
     * @param targetNode The target position
     * @return List of nodes representing the optimal path, or null if no path exists
     */
    public List<Node> findPath(Node startNode, Node targetNode) {

        // Open list: stores nodes to be evaluated, sorted by fCost
        PriorityQueue<Node> openSet = new PriorityQueue<>();
        // Closed list: stores nodes already evaluated
        Set<Node> closedSet = new HashSet<>();

        // Map to store Node objects created, useful for consistent references
        // and retrieving nodes by coordinates
        Map<String, Node> allNodes = new HashMap<>();

        // Initialize start node
        startNode.gCost = 0;
        startNode.fCost = calculateHeuristic(startNode, targetNode);
        openSet.add(startNode);
        allNodes.put(startNode.x + "," + startNode.y, startNode);

        // Possible movements (up, down, left, right)
        // For diagonal movement, add: {-1,-1}, {-1,1}, {1,-1}, {1,1}
        int[] dx = {0, 0, 1, -1};
        int[] dy = {1, -1, 0, 0};

        while (!openSet.isEmpty()) {
//           CommUtils.outgreen("openSet before=" + openSet);
           Node current = openSet.poll(); // Get node with lowest fCost          
//           CommUtils.outmagenta("selected current=" + current);
//           CommUtils.outgreen("openSet after=" + openSet);
//           CommUtils.outgreen("closedSet=" + closedSet);
 
            // If we reached the target
            if (current.equals(targetNode) ) {
            	if ( PlanDBuildDelay > 0 ) {
	            	gui.showCurrentPath( current,targetNode );
	            	CommUtils.delay(FoundPathDelay); //(B)
            	}
                return reconstructPath(current);
            }else { //ADDED JUNE25
            	if(PlanDBuildDelay > 0) gui.showCurrentPath( current,targetNode );
            }
            closedSet.add(current); // Move current to closed set
            //displayOnGui(current.y,current.x,GuiColors.WHITE);

            // Explore neighbors
            for (int i = 0; i < 4; i++) { // Change 4 to 8 for diagonal movement
                int neighborX = current.x + dx[i];
                int neighborY = current.y + dy[i];

                // Check if neighbor is within grid bounds and not an obstacle (1 in grid represents obstacle)
                if (neighborX >= 0 && neighborX < NR &&
                    neighborY >= 0 && neighborY < NC &&
                    grid[neighborX][neighborY] == 0) { // Assuming 0 is traversable, 1 is obstacle

                    Node neighbor = allNodes.getOrDefault(
                    	neighborX + "," + neighborY, new Node(neighborX, neighborY));
                    allNodes.put(neighborX + "," + neighborY, neighbor); // Ensure it's in our map for consistent access

                    if (closedSet.contains(neighbor)) {
//                    	CommUtils.outyellow("already evaluated (since in closedSet):" + neighbor);
                        continue; // Already evaluated this neighbor
                    }

                    // Calculate tentative gCost for neighbor
                    double tentativeGCost = current.gCost + 1; // Assuming cost of 1 to move to adjacent cell

                    // If we found a shorter path to this neighbor
                    if (tentativeGCost < neighbor.gCost) {
                        neighbor.parent = current;
                        neighbor.gCost = tentativeGCost;
                        neighbor.hCost = calculateHeuristic(neighbor, targetNode);
                        neighbor.fCost = neighbor.gCost + neighbor.hCost;

                        if (!openSet.contains(neighbor)) { // Add to openSet if not already there
//                            CommUtils.outblue(i + " adding in openSet neighbor:" + neighbor + " tentativeGCost=" + tentativeGCost );
                            openSet.add(neighbor);
                            //displayOnGui(neighborY,neighborX,GuiColors.WHITE);
                            
                        } else {
                            // If it's already in openSet but we found a better path,
                            // we need to update its position in the PriorityQueue.
                            // The easiest way is to remove and re-add.
                            openSet.remove(neighbor);
                            openSet.add(neighbor);
//                            CommUtils.outmagenta("removeeeeeeeeeeeeeeeeeeeeeeeeee" + neighborY + " " + neighborX);
                            //displayOnGui(neighborY,neighborX,GuiColors.GREEN);
                            //displayOnGui(neighborY,neighborX,GuiColors.WHITE);
                        }
                        
                    }
                }
                CommUtils.delay(PlanDBuildDelay); //(B)
            }//for
        }
        return null; // No path found
    }

    // Reconstructs the path from the target node back to the start node
    private List<Node> reconstructPath(Node current) {
        List<Node> path = new ArrayList<>();
        while (current != null) {
            path.add(0, current); // Add to the beginning to get path in correct order
            current = current.parent;
        }
        return path;
    }
    

    public void printPath(List<Node> path) {
        for (Node node : path) {
            System.out.print(node + " -> ");
        }   	
        System.out.println();
    }

    public String planForGoal(  String x0, String y0, String x, String y) {
    	return planForGoal( Integer.parseInt(x0),
    			Integer.parseInt(y0),
    			Integer.parseInt(x),
    			Integer.parseInt(y)
    			);
    }

    	
    public String planForGoal(  int x0, int y0, int x, int y) {
    	start  = new Node(x0,y0);  //la inversione delle coordinate la fa solo la gui
    	target = new Node(x,y);
    	return calculatePath();
    }
 

    public String calculatePath( ) {
    	String moves = "''";
    	//gui.showTheRoom( );
           List<Node> path = null;       
           path = findPath( start, target);

           if (path != null) {
               moves = robotposutil.FromPathToMoves(path,start,target);
               //lwwwwrwlwrwwrw
//               CommUtils.outmagenta("Movesssssssssssssssssssssssssssssss="+moves);
           } else {
               CommUtils.outred("AStarPathfinding | calculatePath No path found for target=" + target);
           }
    	   return moves;
    }
    
    public void impossiblePath() {
       Node impossibleStart  = new Node(0, 0);
       Node impossibleTarget = new Node(2, 4); // Target is an obstacle
 
      System.out.println("target="+impossibleTarget);
      List<Node> impossiblePath = findPath( impossibleStart, impossibleTarget);
      if (impossiblePath != null) {
          CommUtils.outred("AStarPathfinding |  Path found for impossible target: " + impossiblePath);
      } else {
    	  CommUtils.outred("AStarPathfinding |  Correctly reported: No path found to an obstacle.");
      }  	
    }
    
    public void doPlan(String moves) {
    	gui.showTheRoom( );
    	robotposutil.doPlan(moves,start,target);
    }

    
    public void setEndNodes(int STARTX, int STARTY, int TARGETX, int TARGETY) {
    	setEndNodes( new Node(STARTX, STARTY) , new Node(TARGETX, TARGETY) );    	
    }
    
//    public static void main(String[] args) {
//    	AStarPathfinding appl = new AStarPathfinding( );
//    	
////    	appl.setEndNodes(new Node(0,0), new Node(4,0));
////    	String movesHomeInput = appl.calculatePath(   );      
//    	String movesHomeInput =  appl.planForGoal(0,0,4,0);
//        //CommUtils.delay(1000);
//        CommUtils.waitTheUser("HIT fot next"); 
//        appl.doPlan(movesHomeInput);
//        CommUtils.delay(1000);
//        
//        
//        
////        appl.setEndNodes(new Node(4,0), new Node(3,4));       	
////        String movesInputTo34 = appl.calculatePath(   );
//        String movesInputTo34 = appl.planForGoal(4,0,3,4);
//        //CommUtils.delay(1000);
// 	    CommUtils.waitTheUser("HIT fot next"); 
//        appl.doPlan(movesInputTo34);
//	    CommUtils.delay(1000);
//	    
//
//       
////        appl.setEndNodes(new Node(3,4), new Node(0,0));    
////        String moves34Home = appl.calculatePath(   );
//	    String moves34Home =   appl.planForGoal(3,4,0,0);
//        //CommUtils.delay(1000);
//        CommUtils.waitTheUser("HIT fot next"); 
//        appl.doPlan(moves34Home);
//   	//appl.impossiblePath();
//               	System.exit(0);
//    }
}


/*
|r, 1, 1, 1, 1, 1, 1, 
|1, 1, X, X, 1, 1, 1, 
|1, 1, 1, 1, X, 1, 1, 
|1, 1, X, X, 1, 1, 1, 
|1, 1, 1, 1, 1, 1, 1, 
|X, X, X, X, X, X, X,     	 
*/
