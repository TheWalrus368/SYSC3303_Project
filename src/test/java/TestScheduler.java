import java.util.ArrayList;
import java.util.List;

public class TestScheduler extends Scheduler {

    public FireEvent taskForDrone;
    public boolean taskReady;
    public FireEvent receivedTaskDrone;
    public boolean taskResponseReceived;

    public final List<FireEvent> eventsSent = new ArrayList<>();
    public final List<FireEvent> responsesReceived = new ArrayList<>();

    public TestScheduler() {
        super("src/main/java/sample_zone.csv");
    }


    public synchronized void receiveFireEvent(FireEvent event) {
        while (taskReady) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        this.taskForDrone = event;
        this.receivedTaskDrone = event;
        this.taskReady = true;
        eventsSent.add(event);
        notifyAll();
    }

    public synchronized void sendDroneAcknowledgment(FireEvent event) {
        while (taskResponseReceived) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        this.receivedTaskDrone = event;
        this.taskResponseReceived = true;
        notifyAll();
    }


    public synchronized FireEvent assignTaskToDrone() {
        while (!taskReady) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        FireEvent task = this.taskForDrone;
        this.taskReady = false;
        notifyAll();
        return task;
    }

    public synchronized FireEvent receiveDroneAcknowledgment() {
        while (!taskResponseReceived) {
            try {
                wait();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        FireEvent response = this.receivedTaskDrone;
        this.taskResponseReceived = false;
        responsesReceived.add(response);
        notifyAll();
        return response;
    }

    public List<FireEvent> getEventsSent() {
        return eventsSent;
    }

    public List<FireEvent> getResponsesReceived() {
        return responsesReceived;
    }
}