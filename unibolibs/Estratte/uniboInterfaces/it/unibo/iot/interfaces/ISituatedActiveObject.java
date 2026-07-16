package it.unibo.iot.interfaces;
import java.util.concurrent.ScheduledExecutorService;

public interface ISituatedActiveObject {
	public void activate(ScheduledExecutorService sched);
}
