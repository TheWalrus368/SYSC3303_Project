public class ModifiedDroneSubsystem extends DroneSubsystem{
    public double droneX;
    public double droneY;
    public FireEvent currentFireEvent;

    /**
     * Constructs a DroneSubsystem with a reference to the Scheduler.
     * Initializes the drone state machine and sets the agent level to full capacity.
     *
     * @param droneID the ID of the drone
     */
    public ModifiedDroneSubsystem(int droneID) {
        super(droneID);
        this.droneX = (BASE_ZONE.getStartX() + BASE_ZONE.getEndX());
        this.droneY = (BASE_ZONE.getStartY() + BASE_ZONE.getEndY());
        this.currentFireEvent = null;
    }

    @Override
    public void simulateDroneTravel(Zone targetZone){
        super.simulateDroneTravel(targetZone);

        this.droneX = (targetZone.getStartX() + targetZone.getEndX());
        this.droneY = (targetZone.getStartY() + targetZone.getEndY());
    }

    public void setCurrentFireEvent(FireEvent fireEvent){
        this.currentFireEvent = fireEvent;
    }

    public boolean currentFireExtinguished() {
        return currentFireEvent != null && currentFireEvent.getRemainingWaterNeeded() <= 0;
    }

    @Override
    public FireEvent getCurrentFireEvent() {
        // Override the getter to return the 'currentFireEvent' field
        // declared within this ModifiedDroneSubsystem class.
        return this.currentFireEvent;
    }

}
