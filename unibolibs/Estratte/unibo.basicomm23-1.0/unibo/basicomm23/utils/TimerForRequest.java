// TimerForRequest.java
// Utility class for handling timeouts and responses in asynchronous requests.
package unibo.basicomm23.utils;

public class TimerForRequest extends Thread {
    // Timeout duration in milliseconds
    private int tout;
    // Flag indicating if the timeout has expired
    private boolean toutExpired = false;
    // Stores the answer received before timeout (if any)
    private String answer       = null;

    // Constructor: initializes the timeout duration
    public TimerForRequest(int tout) {
        this.tout=tout;
    }
    // Waits until the timeout expires or an answer is received
    public synchronized String waitTout() throws InterruptedException {
        while ( ! toutExpired ) wait();
        return answer;
    }
    // Sets the timeout as expired and notifies waiting threads
    public synchronized void setExpired( ) {
        toutExpired = true;
        notifyAll();
    }
    // Sets the timeout as expired due to receiving an answer, stores the answer, and notifies waiting threads
    public synchronized void setExpiredSinceAnswer( String answer ) {
        toutExpired = true;
        this.answer = answer;
        notifyAll();
    }

    // Runs the timer thread, waits for the specified timeout, then marks as expired
    public void run() {
        CommUtilsOrig.delay(tout );
        //CommUtils.outmagenta("TIMEOUT");
        setExpired();
    }
}