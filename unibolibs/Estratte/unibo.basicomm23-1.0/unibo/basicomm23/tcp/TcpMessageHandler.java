// TcpMessageHandler.java
// Handles incoming TCP messages using a user-defined handler and a connection.
package unibo.basicomm23.tcp;

import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.IApplMsgHandler;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;
import unibo.basicomm23.utils.CommUtilsOrig;

/*
 * Active entity for receiving messages on an Interaction2021 connection.
 */
public class TcpMessageHandler extends Thread{
    // User-defined message handler
    private IApplMsgHandler handler ;
    // TCP connection
    private Interaction  conn;

    // Constructor: initializes handler and connection, then starts the thread
    public TcpMessageHandler(IApplMsgHandler handler, Interaction conn ) {
        this.handler = handler;
        this.conn    = conn;
        //CommUtils.outblue("TcpMessageHandler | STARTING with user handler:" + handler.getName()  );
        this.start();
    }
    
    @Override 
    public void run() {
        String name = handler.getName();
        // Log start of handler
        CommUtilsOrig.outyellow(getName() + " | TcpMessageHandler  STARTS with user-handler=" + name + " conn=" + conn );
        while( true ) {
            try {
                // Wait for incoming message
                //CommUtils.outblue(name + " | waits for message  ...");
                String msg = conn.receiveMsg();
                CommUtilsOrig.outblue(name + "  | TcpMessageHandler received:" + msg   );
                if( msg == null ) {
                    // If message is null, exit loop (connection closed)
                    //conn.close();    //Feb23
                    break;
                } else{ 
                    // Parse and handle the received message
                    IApplMessage m = new ApplMessage(msg);
                    handler.elaborate( m, conn ); 
                }
            }catch( Exception e) {
                // Log any exception during message handling
                CommUtilsOrig.outred( getName() + "  | TcpMessageHandler: " + e.getMessage()  );
            }
        }
        //CommUtils.outblue(getName() + " | TcpMessageHandler BYE"   );
    }
}