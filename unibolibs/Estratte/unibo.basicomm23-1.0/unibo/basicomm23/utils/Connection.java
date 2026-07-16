package unibo.basicomm23.utils;

// Import required interfaces and message classes
import unibo.basicomm23.interfaces.IApplMessage;
import unibo.basicomm23.interfaces.Interaction;
import unibo.basicomm23.msg.ApplMessage;

// Abstract base class for protocol connections implementing Interaction
public abstract class Connection implements Interaction {
    // Enable or disable tracing of communication
    public boolean trace = false;

    public void setTrace( boolean v) {
    	trace = v;
    }
    // Forward an application message (object) by converting to string
    @Override
    public void forward(IApplMessage msg) throws Exception {
        forward(msg.toString());
    }

    // Send a request and return the response as an application message
    @Override
    public IApplMessage request(IApplMessage msg) throws Exception {
        String answer = request(msg.toString());  // Abstract method blocking
        try {
        	//CommUtilsOrig.outred("Connection | requesttttttttttt answer  :" + answer);
            return new ApplMessage(answer);
        } catch (Exception e) {
            CommUtilsOrig.outred("Connection | request answer no IApplMessage:" + answer);
            IApplMessage replyErr = CommUtils.buildReply(msg.msgReceiver(), "error", answer, msg.msgSender());
            return replyErr;
        }
    }

    // Send a request with a timeout and return the response as an application message
    @Override
    public IApplMessage request(IApplMessage msg, int tout) throws Exception {
        TimerForRequest t = new TimerForRequest(tout);
        t.start();

        // Start a thread to handle the request asynchronously
        new Thread() {
            public void run() {
                try {
                    String answer = request(msg.toString());
                    // CommUtils.outmagenta("request with tout answer:" + answer);
                    t.setExpiredSinceAnswer(answer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.start();

        String answer = t.waitTout();
        if (answer == null) throw new Exception("request timeout");
        else return new ApplMessage(answer);
    }

    // Receive a message and convert it to an application message
    @Override
    public IApplMessage receive() throws Exception {
        String msg = receiveMsg();
        return new ApplMessage(msg);
    }

    // Reply to a message by converting to string
    @Override
    public void reply(IApplMessage msg) throws Exception {
        reply(msg.toString());
    }

    // Abstract method to forward a message (string)
    @Override
    public abstract void forward(String msg) throws Exception;

    // Abstract method to send a request and get a response (string)
    @Override
    public abstract String request(String msg) throws Exception;

    // Abstract method to reply to a request by ID (string)
    @Override
    public abstract void reply(String reqid) throws Exception;

    // Abstract method to receive a message (string)
    @Override
    public abstract String receiveMsg() throws Exception;

    // Abstract method to close the connection
    @Override
    public abstract void close() throws Exception;
}