import java.util.ArrayList;

public class Scheduler implements Runnable {
    private final ArrayList<FireEvent> taskQueue; // Queue for tasks to be assigned to the DroneSubsystem
    private final ArrayList<FireEvent> acknowledgementQueue; // Queue for responses from the DroneSubsystem

    public Scheduler() {
        this.taskQueue = new ArrayList<>();
        this.acknowledgementQueue = new ArrayList<>();
    }

    // Synchronized method for FireIncidentSubsystem to send events to the Drone Subsystem
    public synchronized void receiveFireEvent(FireEvent event) {
        taskQueue.add(event); // Add the event to the queue
        System.out.println("[Scheduler][FIRE_ID: "+event.getFireID()+"] Received a new Fire Event. Event added to task queue: " + event);

        // Notify waiting threads that a new task is available
        notifyAll();
    }

    // Synchronized method for Drone Subsystem to send events to the FireIncidentSubsystem
    public synchronized void sendDroneAcknowledgement(FireEvent event) {
        acknowledgementQueue.add(event); // Add the response to the queue
        System.out.println("[Scheduler][FIRE_ID: "+event.getFireID()+"] Received drone acknowledgement, Response" + " added to acknowledgement queue: " + event);

        // Notify waiting threads that a response is available
        notifyAll();
    }

    // Synchronized method for DroneSubsystem to fetch tasks
    public synchronized FireEvent assignTaskToDrone() {
        while (taskQueue.isEmpty()) { // Wait until a task is available
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        // Fetch and remove the first task from the queue
        FireEvent task = taskQueue.remove(0);
        System.out.println("[Scheduler] Assigning task to DroneSubsystem: " + task);
        
        return task;
    }

    // Synchronized method for receiving response from DroneSubsystem
    public synchronized FireEvent receiveDroneAcknowledgement() {
        while (acknowledgementQueue.isEmpty()) { // Wait until a response is available
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }

        // Fetch and remove the first response from the queue
        FireEvent response = acknowledgementQueue.remove(0);
        System.out.println("[Scheduler] Sending response to FireIncidentSubsystem: " + response);
        return response;
    }

    @Override
    public void run() {
        System.out.println("[Scheduler] Running... Ready to queue and process events.\n");
    }
}
