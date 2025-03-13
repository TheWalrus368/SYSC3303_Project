import java.util.ArrayList;

/**
 * The Scheduler class acts as a mediator between the FireIncidentSubsystem and the DroneSubsystem.
 * It manages task assignments and acknowledgments between these subsystems using synchronized methods.
 */
public class Scheduler implements Runnable {
    private final ArrayList<FireEvent> taskQueue; // Queue for tasks to be assigned to the DroneSubsystem
    private final ArrayList<FireEvent> acknowledgementQueue; // Queue for responses from the DroneSubsystem
    private final SchedulerStateMachine stateMachine;
    /**
     * Constructs a Scheduler instance.
     * Initializes task and acknowledgment queues.
     */
    public Scheduler() {
        this.taskQueue = new ArrayList<>();
        this.acknowledgementQueue = new ArrayList<>();
        this.stateMachine = new SchedulerStateMachine(this);
    }

    /**
     * Receives a fire event from the FireIncidentSubsystem and adds it to the task queue.
     * Notifies any waiting threads that a new task is available.
     * 
     * @param event The fire event to be added to the task queue.
     */
    public synchronized void receiveFireEvent(FireEvent event) {
        taskQueue.add(event); // Add the event to the queue
        stateMachine.setState("RECEIVING");
        try {
            stateMachine.handleState();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[FIRE_ID: "+event.getFireID()+"] Received a new Fire Event. Event added to task queue: " + event);

        // Notify waiting threads that a new task is available
        notifyAll();
    }

    /**
     * Sends an acknowledgment from the DroneSubsystem to the FireIncidentSubsystem.
     * Adds the response to the acknowledgment queue and notifies waiting threads.
     * 
     * @param event The fire event that has been processed by the drone.
     */
    public synchronized void sendDroneAcknowledgement(FireEvent event) {
        acknowledgementQueue.add(event); // Add the response to the queue
        stateMachine.setState("RECEIVING");
        try {
            stateMachine.handleState();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("[FIRE_ID: "+event.getFireID()+"] Received drone acknowledgement, Response" + " added to acknowledgement queue: " + event);

        // Notify waiting threads that a response is available
        notifyAll();
    }

   /**
     * Assigns a fire event task to the DroneSubsystem. If no task is available, it waits until one is received.
     * 
     * @return The fire event assigned to the drone.
     */
    public synchronized FireEvent assignTaskToDrone() {
        while (taskQueue.isEmpty()) { // Wait until a task is available
            try {
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        stateMachine.setState("SEND");
        try {
            stateMachine.handleState();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        // Fetch and remove the first task from the queue
        FireEvent task = taskQueue.removeFirst();
        System.out.println(" Assigning task to DroneSubsystem: " + task);
        
        return task;
    }

    /**
     * Receives acknowledgment from the DroneSubsystem. If no acknowledgment is available, it waits until one is received.
     * 
     * @return The acknowledgment fire event received from the drone.
     */
    public synchronized FireEvent receiveDroneAcknowledgement() {
        while (acknowledgementQueue.isEmpty()) { // Wait until a response is available
            try {
                wait();
            } catch (InterruptedException e) {
            }
        }
        stateMachine.setState("SEND");
        try {
            stateMachine.handleState();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Fetch and remove the first response from the queue
        FireEvent response = acknowledgementQueue.removeFirst();
        System.out.println(response);
        return response;
    }

    /**
     * The main run method for the Scheduler thread. Prints a message indicating it is running.
     */
    @Override
    public void run() {
        System.out.println("[Scheduler] Running... Ready to queue and process events.\n");
        try {
            stateMachine.handleState();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
