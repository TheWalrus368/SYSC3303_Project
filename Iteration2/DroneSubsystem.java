public class DroneSubsystem implements Runnable {
    private final Scheduler scheduler;
    private final DroneStateMachine stateMachine;
    private int agentLevel;
    private FireEvent lastFireEvent; // Store last fire event (in case the fire has not been fully put out)
    private boolean fireEventComplete; 

    private static final int MAX_AGENT_CAP = 15; // Max payload is 15kg

    public DroneSubsystem(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.stateMachine = new DroneStateMachine(this);
        this.agentLevel = MAX_AGENT_CAP; // Start with full agent
        this.fireEventComplete = false;
    }

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

    public boolean isAgentEmpty() {
        return agentLevel == 0;
    }

    public void refillAgent() {
        agentLevel = MAX_AGENT_CAP;
    }

    public int getAgentLevel() {
        return agentLevel;
    }

    public void notifyArrival(FireEvent event, String state) {
        if (event != null) {
            System.out.println("[DroneSubsystem][STATE:"+state+"] Drone has arrived at fire location: " + event);
            scheduler.sendDroneAcknowledgement(event); // Notify the scheduler
        }
    }

    public void notifyReturn(FireEvent event, String state) {
        if (event != null) {
            System.out.println("[DroneSubsystem][STATE:"+state+"] Drone has returned to the base from: " + event);
            scheduler.sendDroneAcknowledgement(event); // Notify the scheduler
        }
    }
    

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

    public void completeTask(FireEvent task) {
        System.out.println("[DroneSubsystem][STATE:DROPPING] Task started with " + task.getRemainingWaterNeeded() + "L remaining to extinguish.");
    
        while (task.getRemainingWaterNeeded() > 0) {
            if (agentLevel > 0) {
                int waterToDrop = Math.min(agentLevel, task.getRemainingWaterNeeded());
                agentLevel -= waterToDrop;
                task.extinguish(waterToDrop);
                System.out.println("[DroneSubsystem][STATE:DROPPING] Dropped: " + waterToDrop + "L of agent. " + agentLevel + "L left.");
            }
    
            if (task.getRemainingWaterNeeded() <= 0) {
                fireEventComplete = true;
                stateMachine.setState("IDLE");
                break;
            }
    
            if (agentLevel == 0) {
                System.out.println("[DroneSubsystem][STATE:DROPPING] Not enough agent to complete the task. Going to refill...");
                stateMachine.setState("REFILLING");
                break;
            }
        }
    
        if (fireEventComplete) {
            System.out.println("[DroneSubsystem][STATE:DROPPING] Sending response back to the Scheduler: " + task);
            scheduler.sendDroneAcknowledgement(task);
        }
    }    
}
