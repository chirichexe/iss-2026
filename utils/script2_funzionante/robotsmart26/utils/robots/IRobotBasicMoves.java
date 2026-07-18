package robots;

interface IRobotBasicMoves {
	//void domove(String move) throws Exception; //move=w | s | l | a | r | d| h | p 
    void turnLeft() throws Exception;
    void turnRight() throws Exception;
    void forward( int time ) throws Exception;
    void backward( int time ) throws Exception;
    void halt() throws Exception;

    //Nuove operazioni
    boolean step(long time) throws Exception;
}
