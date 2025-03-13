/**
 * The DroneSubsystem class represents a drone responsible for handling fire-fighting tasks.
 * It interacts with the Scheduler to receive fire events, manage its agent level, and complete tasks.
 * This class runs as a separate thread and transitions through states using the DroneStateMachine.
 */
public class DroneSubsystem implements Runnable {
    private int droneID;
    private final Scheduler scheduler;
    private final DroneStateMachine stateMachine;
    private int agentLevel;
    private FireEvent lastFireEvent; // Store last fire event (in case the fire has not been fully put out)
    private boolean fireEventComplete; 

    private static final int MAX_AGENT_CAP = 15; // Max payload is 15kg

    /**
     * Constructs a DroneSubsystem with a reference to the Scheduler.
     * Initializes the drone state machine and sets the agent level to full capacity.
     * 
     * @param scheduler The Scheduler instance managing fire events.
     */
    public DroneSubsystem(Scheduler scheduler, int droneID) {
        this.droneID = droneID;
        this.scheduler = scheduler;
        this.stateMachine = new DroneStateMachine(this);
        this.agentLevel = MAX_AGENT_CAP; // Start with full agent
        this.fireEventComplete = false;
    }

    /**
     * The main execution loop for the drone.
     * It continuously processes state transitions using the state machine.
     */
    @Override
    public void run() {
        try {
            while (true) {
                // Handle state from DroneStateMachine
                stateMachine.handleState();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getDroneID(){ return droneID; }
    
    /**
     * Checks if the agent tank is empty.
     * 
     * @return true if the agent tank is empty, false otherwise.
     */
    public boolean isAgentEmpty() {
        return agentLevel == 0;
    }

    /**
     * Refills the agent tank to its maximum capacity.
     */
    public void refillAgent() {
        agentLevel = MAX_AGENT_CAP;
    }

    /**
     * Gets the current agent level.
     * 
     * @return The current agent level in kg.
     */
    public int getAgentLevel() {
        return agentLevel;
    }

    /**
     * Notifies the scheduler when the drone arrives at a fire location.
     * 
     * @param event The fire event the drone is responding to.
     * @param state The current state of the drone.
     */
    public void notifyArrival(FireEvent event, String state) {
        if (event != null) {
            System.out.println("[DroneSubsystem][STATE:"+state+"] Drone has arrived at fire location: " + event);
            scheduler.sendDroneAcknowledgement(event); // Notify the scheduler
        }
    }

    /**
     * Notifies the scheduler when the drone returns to base.
     * 
     * @param event The fire event the drone was responding to.
     * @param state The current state of the drone.
     */
    public void notifyReturn(FireEvent event, String state) {
        if (event != null) {
            System.out.println("[DroneSubsystem][STATE:"+state+"] Drone has returned to the base from: " + event);
            scheduler.sendDroneAcknowledgement(event); // Notify the scheduler
        }
    }

     /**
     * Fetches a fire event task from the scheduler.
     * If the last fire event is not yet fully extinguished, the drone will continue with it.
     * 
     * @return The fire event task assigned to the drone.
     */
    public FireEvent fetchTask() {
        // If there was a previous fire event that has not been fully put out,
        // return this event so that the drone returns to it
        if (lastFireEvent != null && !fireEventComplete) {
            return lastFireEvent;
        } else {
            // Fetch a new task if no previous fire event or fire was put out
            FireEvent newTask = scheduler.assignTaskToDrone();
            lastFireEvent = newTask;
            fireEventComplete = false; // Reset flag for new task
            return newTask;
        }
    }

     /**
     * Completes a fire event task by dropping the extinguishing agent.
     * If the fire is not fully extinguished, the drone may need to refill and return.
     * 
     * @param task The fire event task being handled.
     */
    public void completeTask(FireEvent task) {
        System.out.println("[DroneSubsystem"+droneID+"][STATE:DROPPING] Task started with " + task.getRemainingWaterNeeded() + "L remaining to extinguish.");
    
        while (task.getRemainingWaterNeeded() > 0) {
            if (agentLevel > 0) {
                int waterToDrop = Math.min(agentLevel, task.getRemainingWaterNeeded());
                agentLevel -= waterToDrop;
                task.extinguish(waterToDrop);
                System.out.println("[DroneSubsystem"+droneID+"][STATE:DROPPING] Dropped: " + waterToDrop + "L of agent. " + agentLevel + "L left.");
            }
    
            if (task.getRemainingWaterNeeded() <= 0) {
                fireEventComplete = true;
                stateMachine.setState("IDLE");
                break;
            }
    
            if (agentLevel == 0) {
                System.out.println("[DroneSubsystem"+droneID+"][STATE:DROPPING] Not enough agent to complete the task. Going to refill...");
                stateMachine.setState("REFILLING");
                break;
            }
        }
    
        if (fireEventComplete) {
            System.out.println("[DroneSubsystem"+droneID+"][STATE:DROPPING] Sending response back to the Scheduler: " + task);
            scheduler.sendDroneAcknowledgement(task);
        }
    }    
}
