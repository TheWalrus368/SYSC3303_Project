import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ModifiedScheduler extends Scheduler {
    public final List<DroneStatus> drones;
    public final List<FireEvent> eventsSent = new ArrayList<>();
    public final List<FireEvent> responsesReceived = new ArrayList<>();
    public BoundedBuffer fireToDroneBuffer;

    public ModifiedScheduler() {
        super();
        this.drones= new ArrayList<>();
        this.fireToDroneBuffer = new BoundedBuffer();
    }

    public List<DroneStatus> getDrones(){
        return this.drones;
    }

    /**
     * Overriding so that the test cases use ModifiedScheduler's drone list
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

    @Override
    public void addSortFires(String requestData){
        // Add the fire request to the buffer
        fireToDroneBuffer.addLast(requestData);

        //System.out.println("BUFFER BEFORE SORTING: " + this.fireToDroneBuffer);

        // Sort the buffer
        BoundedBuffer sortedFireBuffer  = new BoundedBuffer();
        List<String> fireEvents         = new ArrayList<>();
        List<String> severityOrder      = Arrays.asList("High", "Moderate", "Low");
        int count                       = fireToDroneBuffer.getCount();

        for (int i = 0; i < count; i++) {
            Object event = fireToDroneBuffer.removeFirst();
            if (event instanceof String) {
                fireEvents.add((String) event);
            }
        }

        // Sorting algorithm for the fire events
        fireEvents.sort(new Comparator<String>() {
            public int compare(String e1, String e2) {
                Pattern pattern = Pattern.compile("severity='(High|Moderate|Low)'");
                Matcher matcher1 = pattern.matcher(e1);
                Matcher matcher2 = pattern.matcher(e2);
                int rank1 = severityOrder.size();
                int rank2 = severityOrder.size();
                if (matcher1.find()) {
                    rank1 = severityOrder.indexOf(matcher1.group(1));
                }
                if (matcher2.find()) {
                    rank2 = severityOrder.indexOf(matcher2.group(1));
                }
                return rank1 - rank2;
            }
        });

        for (String event : fireEvents) {
            sortedFireBuffer.addLast(event);
        }

        // Set the buffer to the sorted buffer
        this.fireToDroneBuffer = sortedFireBuffer;

        //try {
        //    Thread.sleep(5000);
        //} catch (InterruptedException e) {
        //    throw new RuntimeException(e);
        //}
    }

    @Override
    public void updateDroneState(int droneID, String newState) {
        synchronized (this.drones) {
            for (DroneStatus drone : this.drones) {
                if (drone.getDroneID() == droneID) {
                    drone.setState(newState);
                    return;
                }
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
