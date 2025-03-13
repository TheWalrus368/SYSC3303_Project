import java.util.HashMap;
import java.util.Map;

interface SchedulerState{
    void handle(SchedulerStateMachine scheduler);
}

class Starting implements SchedulerState{
    @Override
    public void handle(SchedulerStateMachine scheduler){
        System.out.println("[Scheduler][STATE:STARTING]");
    }
}

class Send implements SchedulerState{
    @Override
    public void handle(SchedulerStateMachine scheduler){
        System.out.print("[Scheduler][STATE:SEND]");
    }
}

class Receive implements SchedulerState{
    @Override
    public void handle(SchedulerStateMachine scheduler){
        System.out.print("[Scheduler][STATE:RECEIVING]");
    }
}

class schedulerFaulted implements SchedulerState{
    @Override
    public void handle(SchedulerStateMachine scheduler){
        try{
            System.out.println("[Scheduler][STATE:FAULT] Scheduler is faulted. Attention required...");
            Thread.sleep(2000);

        } catch (InterruptedException e) {
            e.printStackTrace();
            scheduler.setState("FAULTED");
        }
    }
}

public class SchedulerStateMachine {
    private final Map<String, SchedulerState> states;
    private SchedulerState currentState;
    private Scheduler scheduler;

    public SchedulerStateMachine (Scheduler scheduler){
        this.scheduler = scheduler;
        states = new HashMap<>();
        states.put("STARTING", new Starting());
        states.put("SEND", new Send());
        states.put("RECEIVING", new Receive());
        states.put("FAULTED", new schedulerFaulted());

        currentState = states.get("STARTING");
    }

    public void handleState() throws InterruptedException{
        currentState.handle(this);
    }

    public void setState(String stateName){
        this.currentState = states.get(stateName);
    }

    public Scheduler getScheduler() { return scheduler; }
}
