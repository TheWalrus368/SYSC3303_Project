import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModifiedScheduler extends Scheduler {
    public List<DroneStatus> drones;
    public final List<FireEvent> eventsSent = new ArrayList<>();
    public final List<FireEvent> responsesReceived = new ArrayList<>();

    public ModifiedScheduler() {
        super();
        this.drones= new ArrayList<>();
    }

    public List<DroneStatus> getDrones(){
        return this.drones;
    }

    /**
     * Overriding so that the test cases use ModifiedScheduler's drone list
     * @return
     */
    @Override
    public synchronized DroneStatus getAvailableDrone() {
        for (DroneStatus drone : this.drones) {  // Use ModifiedScheduler's drones list
            if (drone.getState().equals("IDLE")) {
                drone.setState("USED");
                return drone;
            }
        }
        return null;
    }

    /**
     * Overriding so that the test drones' status are being added to the ModifiedScheduler,
     * not Scheduler class
     * @param data The incoming data string to be parsed.
     * @return
     */
    @Override
    public EventStatus handleEvent(String data) {
        String pattern = "\\[DRONE: (\\d+)]\\[PORT: (\\d+)]\\[STATE: ([^]]+)]";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(data);

        // Drone event
        if (matcher.find()) {
            int droneID = Integer.parseInt(matcher.group(1));
            int port = Integer.parseInt(matcher.group(2));
            String state = matcher.group(3);

            // Drone faulting
            if (data.contains("FAULT")) {
                return new EventStatus("FAULT");
            }

            // If not a fault, handle the drone event
            DroneStatus newDrone = new DroneStatus(droneID, port, state, null);

            synchronized (this.drones) {  // Use the drones list of the ModifiedScheduler
                for (DroneStatus existingDrone : this.drones) {
                    if (existingDrone.getDroneID() == newDrone.getDroneID()) {
                        // An existing drone has completed its fire service
                        if (newDrone.getState().equals("COMPLETE")) {
                            return new EventStatus("COMPLETE", newDrone);
                        }
                    }
                }
                // New drone, register it to the list of drones in ModifiedScheduler
                this.drones.add(newDrone);
            }
            // Create and return a new event to handle a ready drone
            return new EventStatus("IDLE", newDrone);
        } else {
            if (data.contains("NEW FIRE")) {
                return new EventStatus("FIRE");
            } else if (data.contains("REQUEST CONFIRMATION:")) {
                return new EventStatus("CONFIRMATION");
            } else {
                // Unrecognized command, should never reach this, otherwise something went wrong
                return new EventStatus("ERROR");
            }
        }
    }

    public List<FireEvent> getEventsSent() {
        return eventsSent;
    }

    public List<FireEvent> getResponsesReceived() {
        return responsesReceived;
    }
}
