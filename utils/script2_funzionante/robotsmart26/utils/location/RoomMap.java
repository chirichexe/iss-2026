package location;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import unibo.basicomm23.utils.CommUtils;

/*
 * ----------------------------------------
 * RoomMap.java
 * ----------------------------------------
 */
public class RoomMap implements Serializable {
	private static final long serialVersionUID = 1L;

	public enum cellvalue {
		UNKNOWN, OBSTACLE, FREE, ROBOT
	}

	public enum Direction {
		DOWN, LEFT, UP, RIGHT
	}
	//Il record Pos è implicitamente statico
	//Trasporto dati puro (DTO)
	public record Pos(int x, int y) implements Serializable { //Se no, no genera bin
        private static final long serialVersionUID = 1L;
    }

	private int nr = 0;
	private int nc = 0;
	private cellvalue[][] roomMap;
	private Direction curDir;
	private Pos curPos;


	public RoomMap() {
		this(6, 5);
	}

	public RoomMap(int nr, int nc) {
		this.nr = nr;
		this.nc = nc;
		roomMap = new cellvalue[nr][nc];
		clear();
	}

	/*
	 * ------------------------------------------
	 * DIRECTioN
	 * ------------------------------------------
	 */
	public void setDir(Direction dir) {
		curDir = dir;
	}

	public void setDirDown() {
		curDir = Direction.DOWN;
	}

	public void setDirLeft() {
		curDir = Direction.LEFT;
	}

	public void setDirUp() {
		curDir = Direction.UP;
	}

	public void setDirRight() {
		curDir = Direction.RIGHT;
	}

	public Direction getDir() {
		return curDir;
	}

	public boolean goingUp() {
		return curDir == Direction.UP;
	}

	public boolean goingDown() {
		return curDir == Direction.DOWN;
	}

	public boolean goingLeft() {
		return curDir == Direction.LEFT;
	}

	public boolean goingRight() {
		return curDir == Direction.RIGHT;
	}
	
	/*
	 * --------------------------------------
	 * POSITION x VERT | y ORIZ
	 * --------------------------------------
	 */
	public void setRobotAtHome( ) {
		setRobotPos( getHome().x,getHome().y, Direction.DOWN, cellvalue.ROBOT ) ;
	}
	public void setRobotPos(int x, int y) {
		setRobotPos(x,y,curDir,cellvalue.ROBOT);
	}
	
	public int getPosX() {
		return curPos.x;
	}
	public int getPosY() {
		return curPos.y;
	}
	public void setRobotPos(int x, int y, Direction dir, cellvalue v ) {
		curPos        = new Pos(x,y);
        curDir        = dir;
		roomMap[curPos.x][curPos.y] = v;
	}

	public Pos getHome() {
		return new Pos(0,0);
	}
    
	public void doStep( ) {
		//WARNING: no check for obstacles, just working in mind
		//CommUtils.outyellow("doStep curDir=" + curDir);
		setCellClean(curPos.x, curPos.y);
		switch (curDir) {
			case DOWN: {
				int x = curPos.x+1;
				int y = curPos.y ;
				CommUtils.outyellow("doStep x,y=" + x + " " + y);
				setRobotPos(x,y);
				break;
			}
			case LEFT: {
				int x =  curPos.x;
				int y = curPos.y - 1;
				setRobotPos(x,y);
				break;
			}
			case UP: {
				int x =  curPos.x - 1;
				int y = curPos.y ;
				setRobotPos(x,y);
				break;
			}
			case RIGHT: {
				int x = curPos.x ;
				int y = curPos.y + 1;
				setRobotPos(x,y);
				break;
			}
		default:
			break;
		};		
	}
	

	/*
	 * ------------------------------------------------------------------
	 * CELLS 
	 *  0 : UNKNOWN 
	 *  1 : free 
	 *  X : obstacle 
	 *  r : robot
	 * ------------------------------------------------------------------
	 */

	public int getNr() {
		return nr;
	}

	public int getNc() {
		return nc;
	}

	public void clear() {
		for (int i = 0; i < nr; i++) {
			for (int j = 0; j < nc; j++) {
				roomMap[i][j] = cellvalue.UNKNOWN;
			}
		}
	}

	public boolean typeOfCell(int x, int y, cellvalue v) {
		return roomMap[x][y] == v;
	}

	public void setCell(int x, int y, cellvalue v) {
		roomMap[x][y] = v;
	}

	public void setCellClean(int x, int y) {
		roomMap[x][y] = cellvalue.FREE;
	}

	public void setRobot(int x, int y) {
		roomMap[x][y] = cellvalue.ROBOT;
	}

	public void setFree(int x, int y) {
		roomMap[x][y] = cellvalue.FREE;
	}

	public void setObstacle(int x, int y) {
		roomMap[x][y] = cellvalue.OBSTACLE;
	}

	/*
	 * ------------------------------------------------ 
	 * MOVES
	 * ------------------------------------------------
	 */

	public void turnLeft() { // Senso AntiOrario
		switch (curDir) {
		case DOWN:
			curDir = Direction.RIGHT;
			break;
		case RIGHT:
			curDir = Direction.UP;
			break;
		case UP:
			curDir = Direction.LEFT;
			break;
		case LEFT:
			curDir = Direction.DOWN;
			break;
		}
	}

	public void turnRight() { // Senso Orario
		switch (curDir) {
		case DOWN:
			curDir = Direction.LEFT;
			break;
		case LEFT:
			curDir = Direction.UP;
			break;
		case UP:
			curDir = Direction.RIGHT;
			break;
		case RIGHT:
			curDir = Direction.DOWN;
			break;
		}
	}
	
	public void step() {
		
	}
	

	/*
	 * -------------------------------------------------------- 
	 * UTILS
	 * --------------------------------------------------------
	 */
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nr; i++) {
			sb.append("|");
			for (int j = 0; j < nc; j++) {
				if (typeOfCell(i, j, cellvalue.ROBOT))
					sb.append("r, ");
				else if (typeOfCell(i, j, cellvalue.OBSTACLE))
					sb.append("X, ");
				else if (typeOfCell(i, j, cellvalue.UNKNOWN))
					sb.append("0, ");
				else if (typeOfCell(i, j, cellvalue.FREE))
					sb.append("1, ");
			}
			sb.append("\n");
		}
		return sb.toString();
	}

	public String toProlog() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < nr; i++) {
			for (int j = 0; j < nc; j++) {
				sb.append("cell(");
				sb.append("" + i + "," + j + ",");
				if (typeOfCell(i, j, cellvalue.ROBOT))
					sb.append("r");
				else if (typeOfCell(i, j, cellvalue.OBSTACLE))
					sb.append("X");
				else if (typeOfCell(i, j, cellvalue.UNKNOWN))
					sb.append("0");
				else if (typeOfCell(i, j, cellvalue.FREE))
					sb.append("1");
				sb.append(").\n");
			}
		}
		return sb.toString();
	}

	public void showMap() {
		String mapStr = toString();
		mapStr = mapStr + " DIR->" + getDir();
		CommUtils.outyellow(mapStr);

	}

	public RoomMap loadRoomMap(String fname) {
		try {
			ObjectInputStream inps = new ObjectInputStream(new FileInputStream(fname + ".bin"));
			RoomMap map = (RoomMap) inps.readObject();
			CommUtils.outyellow("loadRoomMap DONE from " + fname);
			// RoomMap.setRoomMap( map );
			inps.close();
			return map;
		} catch (Exception e) {
			CommUtils.outred("loadRoomMap FAILURE " + e.getMessage());
			return null;
		}
	}

	public void saveRoomMap(String fname) throws Exception {
		saveRoomMap(fname, toString());
	}

	public void saveRoomMap(String fname, String map) throws Exception {
		CommUtils.outyellow("saveRoomMap in " + fname);
		PrintWriter pw = new PrintWriter(new FileWriter(fname + ".txt"));
		pw.print(map);
		pw.close();

		ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(fname + ".bin"));
		os.writeObject(this);
		os.flush();
		os.close();
	}

	public void saveRoomMapProlog(String fname) {
		try {
			saveRoomMapProlog(fname, toString());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveRoomMapProlog(String fname, String map) throws Exception {
		CommUtils.outyellow("saveRoomMap in " + fname);
		PrintWriter pw = new PrintWriter(new FileWriter(fname + ".pl"));
		pw.print(toProlog());
		pw.close();
	}

}
