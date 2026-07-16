package unibo.basicomm23.tcp;

// Import required classes for TCP communication
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.utils.ColorsOut;
import unibo.basicomm23.utils.CommUtils;
import unibo.basicomm23.utils.CommUtilsOrig;
import unibo.basicomm23.utils.Connection;

// TCP connection class implementing the Interaction interface
public class TcpConnection extends Connection { //implements Interaction
    // Output channel for sending data
    private DataOutputStream outputChannel;
    // Input channel for receiving data
    private BufferedReader inputChannel;
    // Underlying socket
    private Socket socket;
    // Port number
    private int port;

    // Factory method to create a TCP connection with retry logic
    public static Interaction create(String host, int port) throws Exception{
        for( int i=1; i<=10; i++ ) {
            try {
                Socket socket      =  new Socket( host, port );
                Interaction  conn  =  new TcpConnection( socket );
                return conn;
            }catch(Exception e) {
                ColorsOut.out("    +++ TcpConnection | Another attempt to connect with host:" + host + " port=" + port);
                Thread.sleep(500);
            }
        }
        CommUtilsOrig.outred("    +++ TcpConnection FAILED with port=" + port);
        //throw new Exception("    +++ TcpConnection ERROR");
        return null;
    }

    // Constructor to create a TCP connection from host and port
    public TcpConnection( String host, int port  ) throws Exception {
        //Socket socket  = new Socket( host, port );
        this( new Socket( host, port ) );
    }

    // Constructor to create a TCP connection from an existing socket
    public TcpConnection( Socket socket  )  throws Exception {
        // Initialize socket and channels
        this.socket = socket;
        OutputStream outStream = socket.getOutputStream();
        InputStream inStream   = socket.getInputStream();
        outputChannel = new DataOutputStream(outStream);
        inputChannel  = new BufferedReader(new InputStreamReader(inStream));
    }
    
    // Send a message to the output channel
    @Override
    public void forward(String msg)  throws Exception {
        if( trace )  
        	CommUtilsOrig.outyellow( "    +++ TcpConnection | sendALine   on " + outputChannel );
        try {
            outputChannel.writeBytes( msg+"\n" );
            outputChannel.flush();
            //socket.shutdownOutput(); //NO
        } catch (IOException e) {
            throw e;
        }    
    }

    // Send a request and wait for a response
    @Override
    public String request(String msg)  throws Exception {
        forward(  msg );
        String answer = receiveMsg();
        //CommUtilsOrig.outred("   +++ TcpConnection | reques answer  :" + answer);
        return answer;
    }

    // Reply to a message (same as forward)
    @Override
    public void reply(String msg) throws Exception {
        forward(msg);
    }

    // Receive a message from the input channel (blocking)
    @Override
    public String receiveMsg()  { //called by TcpApplMessageHandler
        try {
 //            if( trace ) 
            	CommUtilsOrig.outyellow( "    +++ TcpConnection | receiveMsg..... " + " in " + Thread.currentThread().getName()  );
            //socket.setSoTimeout(timeOut)
            String line = inputChannel.readLine() ; //blocking =>
//            String xxx  = "";
//            while ((xxx = inputChannel.readLine()) != null) {
//                System.out.println("    +++ TcpConnection | Ricevuto xxx: " + xxx + " line=" + line);
//            }
             if( trace ) 
            	CommUtilsOrig.outyellow( "    +++ TcpConnection | receiveMsg on port:" + socket.getPort()
                    + " " +line + " thname=" + Thread.currentThread().getName() );
            //CASO NON PIU POSSIBILE April 2026
            if( line.length() == 0 ) {
            	CommUtils.outblack("perhaps n");
            	return receiveMsg();
            }
            return line;
        } catch ( Exception e   ) {
            CommUtilsOrig.outred( "    +++ TcpConnection | receiveMsg  ERROR:" + e.getMessage() );
            return null;
        }        
    }

    // Close the TCP connection and release resources
    @Override
    public void close() {  //called by TcpApplMessageHandler
        try {
            socket.close();
            if( trace ) CommUtilsOrig.outyellow( "    +++ TcpConnection | CLOSED port=" + socket.getPort() );
        } catch (IOException e) {
            CommUtilsOrig.outred( "    +++ TcpConnection | close ERROR " + e.getMessage());
        }
    }



}