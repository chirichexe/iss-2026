package gui;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import unibo.basicomm23.utils.CommUtils;

public class MapUtil {

    protected int NR = 0;
    protected int NC = 0;
    protected String map = "";
    protected int[][] grid;
    
      

	public int[][] createGridFromMapInFile( String fname ) {
		map         = loadMap(fname);
		String[] m1 = map.split("@");
		NR = m1.length; 
		NC = m1[0].length();
		CommUtils.outblue("createGridFromMapInFile NR=" + NR + " NC="+ NC);
		createTheGrid(m1);
 //		showTheGrid();		
		return grid;
	}

	public void createTheGrid( String[] rows ) {
		grid = new int[NR][NC];
		for( int i=0; i < NR; i++) {
			for( int j=0; j < NC ; j++ ) {
				int v = Integer.parseInt(""+rows[i].charAt(j));
				grid[i][j] = v;
			}
			//CommUtils.outgreen("row_"+ i + ") " + rows[i]);			
		}
	}


    public String loadMap( String fname ){   
        try{
        	CommUtils.outyellow("loadMap  from "+fname);
        	Path percorsoFile = Paths.get(fname);
        	String map = Files.readString(percorsoFile);
        	CommUtils.outyellow("map="+ map);
        	return map;
        }catch(Exception e){
        	CommUtils.outred("loadMap FAILURE "+ e.getMessage());
        	return null;
        }
    }    

	public void showTheGrid(  ) {
		for( int i=0; i < NR; i++) {
			for( int j=0; j < NC ; j++ ) {
				System.out.print(  ""+grid[i][j]);
			}
			System.out.println();
		}		
	}

	public static void main(String[] args) {
		MapUtil appl = new MapUtil();
		appl.createGridFromMapInFile("tf25map.txt");
	}

}
