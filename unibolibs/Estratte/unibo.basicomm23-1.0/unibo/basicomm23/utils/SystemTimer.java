package unibo.basicomm23.utils;

// Utility class for measuring elapsed time between events
public class SystemTimer { //extends Thread
    // Stores the start time in milliseconds
    private long startTime;
    // Stores the end time in milliseconds
    private long endTime;
    // Stores the duration between start and end time
    private long duration; 

    // Starts the timer by recording the current system time
    public void startTime() {
        startTime = System.currentTimeMillis();
    }
    // Stops the timer and calculates the duration
    public void stopTime() {
        endTime = System.currentTimeMillis();
        duration = endTime - startTime ;
    }
    // Returns the measured duration in milliseconds
    public long getDuration() {
        return duration;
    }
 
}