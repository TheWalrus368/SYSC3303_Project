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

    }
}
/**
 * State representing the drone travelling to the fire incident location.
 */
class EnRoute implements DroneState {
    @Override
    public void handle(DroneStateMachine context) {
        DroneSubsystem drone = context.getDrone();
    }
}

/**
 * State representing the drone dropping fire-suppressing agent at the fire location.
 */
class DroppingAgent implements DroneState {
    @Override
    public void handle(DroneStateMachine context){
        DroneSubsystem drone = context.getDrone();
    }
}

/**
 * State representing the drone refilling its fire-suppressing agent.
 */
class Refilling implements DroneState {
    @Override
    public void handle(DroneStateMachine context){
        DroneSubsystem drone = context.getDrone();
    }
}

/**
 * State representing a fault or error in the drone's operation.
 */
class Faulted implements DroneState {
    @Override
    public void handle(DroneStateMachine context){
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
        states.put("FAULTED", new Faulted());

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

    public String getState(String stateName){
        return currentState.getClass().getSimpleName();
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
