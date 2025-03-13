import java.util.HashMap;
import java.util.Map;

/**
*Interface representing a state in the DroneStateMachine
**/
interface DroneState {
     /**
     * Handles the logic of the current state and determines the next state.
     *
     * @param context The DroneStateMachine context.
     */
    void handle(DroneStateMachine context);
}

/**
 * State representing the idle mode of the drone.
 */
class Idle implements DroneState {
    @Override
    public void handle(DroneStateMachine context) {
        DroneSubsystem drone = context.getDrone();
        System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:IDLE] Idling...");

        try {
            // If agent is empty, refill
            if(drone.isAgentEmpty()){
                context.setState("REFILLING");
            } else {
                // Fetch task from Scheduler
                FireEvent task = drone.fetchTask();
                System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:IDLE] Received task: " + task);
                
                // Simulate task processing
                Thread.sleep(2000);
                context.setState("EN_ROUTE");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            context.setState("FAULTED");
        }
    }
}
/**
 * State representing the drone travelling to the fire incident location.
 */
class EnRoute implements DroneState {
    @Override
    public void handle(DroneStateMachine context) {
        try {
            DroneSubsystem drone = context.getDrone();
            FireEvent task = drone.fetchTask(); // Fetch the current assigned task
            
            System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:EN_ROUTE] En route to fire incident...");

            Thread.sleep(2000); // Simulating travel time

            // Notify the scheduler that the drone has arrived
            if (task != null) {
                drone.notifyArrival(task, "EN_ROUTE");
            }

            // Transition to the next state
            context.setState("DROPPING_AGENT");
        } catch (InterruptedException e) {
            e.printStackTrace();
            context.setState("FAULTED");
        }
    }
}

/**
 * State representing the drone dropping fire-suppressing agent at the fire location.
 */
class DroppingAgent implements DroneState {
    @Override
    public void handle(DroneStateMachine context){
        try{
            DroneSubsystem drone = context.getDrone();
            FireEvent task = drone.fetchTask();

            System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "] Dropping agent...");
            Thread.sleep(2000);

            // Simulate dropping agent and depletion
            drone.completeTask(task);

            if (drone.isAgentEmpty() && task.getRemainingWaterNeeded() > 0) {
                System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:DROPPING] Not enough agent, refilling...");
                context.setState("REFILLING");
            } else{
                System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:DROPPING] Fire is out, returning to base.");
                drone.notifyReturn(task, "DROPPING");
                context.setState("IDLE");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
            context.setState("FAULTED");
        }
    }
}

/**
 * State representing the drone refilling its fire-suppressing agent.
 */
class Refilling implements DroneState {
    @Override
    public void handle(DroneStateMachine context){
        try{
            DroneSubsystem drone = context.getDrone();
            FireEvent lastTask = drone.fetchTask();

            if (drone.isAgentEmpty()) {
                drone.notifyReturn(lastTask, "REFILLING");
                System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:REFILLING] Refilling agent...");
                Thread.sleep(2000);
                drone.refillAgent();
                System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:REFILLING] Agent refilled.");
            } else {
                System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:REFILLING] Skipping refill, agent level: " + drone.getAgentLevel() + "L");
            }

            if (lastTask != null && lastTask.getRemainingWaterNeeded() > 0) {
                drone.notifyArrival(lastTask, "REFILLING");
                System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:REFILLING] Returned to handle fire...");
                context.setState("DROPPING_AGENT");
            } else {
                context.setState("IDLE");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
            context.setState("FAULTED");
        }
    }
}

/**
 * State representing a fault or error in the drone's operation.
 */
class droneFaulted implements DroneState {
    @Override
    public void handle(DroneStateMachine context){
        try{
            System.out.println("[DroneSubsystem" + context.getDrone().getDroneID() + "][STATE:FAULT] Drone is faulted. Attention required...");
            Thread.sleep(2000);
        
        } catch (InterruptedException e) {
            e.printStackTrace();
            context.setState("FAULTED");
        }
    }
}

/**
 * The DroneStateMachine manages the drone's states and transitions between them.
 */
public class DroneStateMachine {
    private final Map<String, DroneState> states;
    private DroneState currentState;
    private DroneSubsystem drone;

    /**
     * Initializes the DroneStateMachine with predefined states.
     *
     * @param drone The DroneSubsystem instance controlling the drone.
     */
    public DroneStateMachine(DroneSubsystem drone){
        this.drone = drone;
        states = new HashMap<>();
        states.put("IDLE", new Idle());
        states.put("EN_ROUTE", new EnRoute());
        states.put("DROPPING_AGENT", new DroppingAgent());
        states.put("REFILLING", new Refilling());
        states.put("FAULTED", new droneFaulted());

        currentState = states.get("IDLE");
    }

    
    /**
     * Executes the logic of the current state.
     *
     * @throws InterruptedException If thread execution is interrupted.
     */
    public void handleState() throws InterruptedException{
        currentState.handle(this);
    }

    /**
     * Transitions the state machine to a new state.
     *
     * @param stateName The name of the new state.
     */
    public void setState(String stateName){
        this.currentState = states.get(stateName);
    }

    /**
     * Retrieves the DroneSubsystem instance.
     *
     * @return The associated DroneSubsystem.
     */
    public DroneSubsystem getDrone(){
        return drone;
    }
}
