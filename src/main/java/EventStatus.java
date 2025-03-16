/**
 * Represents the status of an event, including the command type and associated drone status.
 * This class is used to capture information about events such as drone readiness,
 * fire completion, or errors.
 */
public class EventStatus {

    private String command;
    private DroneStatus droneStatus = null;

    /**
     * Constructs an EventStatus with the specified command.
     *
     * @param command The type of event (e.g., "READY", "COMPLETE", "FIRE", "ERROR").
     */
    public EventStatus(String command){
        this.command = command;
    }

    /**
     * Constructs an EventStatus with the specified command and associated drone status.
     *
     * @param command     The type of event (e.g., "READY", "COMPLETE", "FIRE", "ERROR").
     * @param droneStatus The status of the drone associated with the event.
     */
    public EventStatus(String command, DroneStatus droneStatus){
        this.command = command;
        this.droneStatus = droneStatus;
    }

    /**
     * @return String The command type (e.g., "READY", "COMPLETE", "FIRE", "ERROR").
     */
    public String getCommand(){
        return this.command;
    }

    /**
     * @return DroneStatus The status of the drone, or null if no drone is associated.
     */
    public DroneStatus getDroneStatus() {
        return this.droneStatus;
    }
}