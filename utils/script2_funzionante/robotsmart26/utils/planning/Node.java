package planning;

import java.util.Objects;

/**
 * Node - Represents a node in the grid for A* pathfinding algorithm
 * 
 * This class represents a single cell/node in the grid environment used for robot pathfinding.
 * Each node contains position coordinates (x,y) and pathfinding costs used by the A* algorithm.
 * The class implements Comparable to enable sorting in priority queues during pathfinding.
 * 
 * Key properties:
 * - Position coordinates (x, y)
 * - Pathfinding costs (gCost, hCost, fCost)
 * - Parent reference for path reconstruction
 * 
 * The A* algorithm uses these costs:
 * - gCost: Actual cost from start to this node
 * - hCost: Heuristic cost from this node to target
 * - fCost: Total cost (gCost + hCost) used for node prioritization
 * 
 * @author Unibo BasicRobot25 Team
 * @version 2025
 */
public class Node implements Comparable<Node> {
    /** X coordinate (column) in the grid */
	public int x;
    /** Y coordinate (row) in the grid */
    public int y;
    /** Actual cost from start node to this node */
    double gCost;
    /** Heuristic cost from this node to target node */
    double hCost;
    /** Total cost (gCost + hCost) used for node prioritization in A* */
    double fCost;
    /** Parent node reference for path reconstruction */
    public Node parent;

    /**
     * Constructor creates a new node with specified coordinates
     * Initializes all costs to 100 (instead of Double.MAX_VALUE for better performance)
     * and sets parent to null
     * 
     * @param x The X coordinate (column) in the grid
     * @param y The Y coordinate (row) in the grid
     */
    public Node(int x, int y) {
        this.x = x;
        this.y = y;
        // Initialize costs to 100 instead of Double.MAX_VALUE for better performance
        this.gCost = 100;
        this.hCost = 100;
        this.fCost = 100;
        this.parent = null;
    }

    /**
     * Compares this node with another based on fCost for priority queue ordering
     * Lower fCost nodes have higher priority in the A* algorithm
     * 
     * @param other The node to compare with
     * @return Negative if this node has lower fCost, positive if higher, 0 if equal
     */
    @Override
    public int compareTo(Node other) {
        return Double.compare(this.fCost, other.fCost);
    }

    /**
     * Checks if this node equals another node based on coordinates
     * Two nodes are considered equal if they have the same x and y coordinates
     * 
     * @param obj The object to compare with
     * @return true if nodes have same coordinates, false otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true; 
        if (obj == null || getClass() != obj.getClass()) return false;
        Node node = (Node) obj;
        return x == node.x && y == node.y;
    }

    /**
     * Generates hash code based on node coordinates
     * Ensures consistent hashing for nodes with same coordinates
     * 
     * @return Hash code based on x and y coordinates
     */
    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    /**
     * Returns string representation of the node
     * Format: "(x, y)" showing the node's coordinates
     * 
     * @return String representation of the node coordinates
     */
    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}

