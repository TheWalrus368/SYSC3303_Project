import java.util.HashMap;
import java.util.Map;

interface DroneState {
    void handle(DroneStateMachine context);
}

class IdleState implements DroneState {
    @Override
    public void handle(DroneStateMachine context) {
        DroneSubsystem drone = context.getDrone();
        System.out.println("[DroneSubsystem] Idling...");

        try {
            // If agent is empty, refill
            if(drone.isAgentEmpty()){
                context.setState("REFILLING");
            } else {
                // Fetch task from Scheduler
                FireEvent task = drone.fetchTask();
                System.out.println("[DroneSubsystem] Received task: " + task);
                
                // Simulate task processing
                Thread.sleep(2000);
                context.setState("EN_ROUTE");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class EnRouteState implements DroneState {
    @Override
    public void handle(DroneStateMachine context) {
        try {
            DroneSubsystem drone = context.getDrone();
            FireEvent task = drone.fetchTask(); // Fetch the current assigned task
            
            System.out.println("[DroneSubsystem] En route to fire incident...");

            Thread.sleep(2000); // Simulating travel time

            // Notify the scheduler that the drone has arrived
            if (task != null) {
                drone.notifyArrival(task);
            }

            // Transition to the next state
            context.setState("DROPPING_AGENT");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


class DroppingAgentState implements DroneState {
    @Override
    public void handle(DroneStateMachine context){
        try{
            DroneSubsystem drone = context.getDrone();
            FireEvent task = drone.fetchTask();

            System.out.println("[DroneSubsystem] Dropping agent...");
            Thread.sleep(2000);

            // Simulate dropping agent and depletion
            drone.completeTask(task);

            if (drone.isAgentEmpty() && drone.getRemainingWaterNeeded() > 0) {
                System.out.println("[DroneSubsystem] Not enough agent, refilling...");
                context.setState("REFILLING");
            } else if (drone.getRemainingWaterNeeded() <= 0) {
                System.out.println("[DroneSubsystem] Fire is out, returning to base.");
                drone.notifyReturn(task);
                context.setState("IDLE");
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class RefillingState implements DroneState {
    @Override
    public void handle(DroneStateMachine context){
        try{
            DroneSubsystem drone = context.getDrone();
            FireEvent lastTask = drone.fetchTask();

            if (drone.isAgentEmpty()) {
                drone.notifyReturn(lastTask);
                System.out.println("[DroneSubsystem] Refilling agent...");
                Thread.sleep(2000);
                drone.refillAgent();
                System.out.println("[DroneSubsystem] Agent refilled.");
            } else {
                System.out.println("[DroneSubsystem] Skipping refill, agent level: " + drone.getAgentLevel() + "L");
            }

            if (lastTask != null && drone.getRemainingWaterNeeded() > 0) {
                drone.notifyArrival(lastTask);
                System.out.println("[DroneSubsystem] Returned to handle fire...");
                context.setState("DROPPING_AGENT");
            } else {
                context.setState("IDLE");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


class FaultedState implements DroneState {
    @Override
    public void handle(DroneStateMachine context){
        try{
            System.out.println("[DroneSubsystem] Drone is faulted. Attention required...");
            Thread.sleep(2000);
        
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

public class DroneStateMachine {
    private final Map<String, DroneState> states;
    private DroneState currentState;
    private DroneSubsystem drone;


    public DroneStateMachine(DroneSubsystem drone){
        this.drone = drone;
        states = new HashMap<>();
        states.put("IDLE", new IdleState());
        states.put("EN_ROUTE", new EnRouteState());
        states.put("DROPPING_AGENT", new DroppingAgentState());
        states.put("REFILLING", new RefillingState());
        states.put("FAULTED", new FaultedState());

        currentState = states.get("IDLE");
    }

    public void handleState() throws InterruptedException{
        currentState.handle(this);
    }

    public void setState(String stateName){
        this.currentState = states.get(stateName);
    }

    public DroneSubsystem getDrone(){
        return drone;
    }
}
