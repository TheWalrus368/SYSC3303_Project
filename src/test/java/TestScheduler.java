import java.util.ArrayList;
import java.util.List;

/**
 * The {@code TestScheduler} class extends the {@code Scheduler} class to facilitate unit testing.
 * It overrides synchronization methods and tracks fire events for verification in test cases.
 */
public class TestScheduler extends Scheduler {
    public FireEvent taskForDrone;
    public boolean taskReady;
    public FireEvent receivedTaskDrone;
    public boolean taskResponseReceived;

    // Lists to track events sent and responses received
    public final List<FireEvent> eventsSent = new ArrayList<>();
    public final List<FireEvent> responsesReceived = new ArrayList<>();

    /**
     * Overrides {@code receiveFireEvent} to store the event and track events sent.
     *
     * @param event The {@code FireEvent} to be sent to the drone subsystem.
     */
    @Override
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
        eventsSent.add(event);  // Track the event sent
        notifyAll();
    }

    /**
     * Overrides {@code returnFireEvent} to store the response from the drone subsystem.
     *
     * @param event The {@code FireEvent} response to be returned.
     */
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

    /**
     * Assigns a fire event task to the drone subsystem.
     *
     * @return The assigned {@code FireEvent}.
     */
    @Override
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

    /**
     * Returns a fire event response to the fire incident subsystem.
     *
     * @return The processed {@code FireEvent} response.
     */
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
        responsesReceived.add(response);  // Track the response received
        notifyAll();
        return response;
    }

    /**
     * Retrieves the list of fire events that were sent.
     *
     * @return A list of {@code FireEvent} objects.
     */
    public List<FireEvent> getEventsSent() {
        return eventsSent;
    }

    /**
     * Retrieves the list of responses received from the drone subsystem.
     *
     * @return A list of {@code FireEvent} objects.
     */
    public List<FireEvent> getResponsesReceived() {
        return responsesReceived;
    }
}