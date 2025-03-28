public class ModifiedDroneSubsystem extends DroneSubsystem{
    /**
     * Constructs a DroneSubsystem with a reference to the Scheduler.
     * Initializes the drone state machine and sets the agent level to full capacity.
     *
     * @param droneID the ID of the drone
     */
    public ModifiedDroneSubsystem(int droneID) {
        super(droneID);
    }

}
