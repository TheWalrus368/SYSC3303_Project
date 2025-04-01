/**
 * Responsible for managing the drone states from the scheduler depending
 * on the event state.
 */
class DroneStatus {
    private final int droneId;
    private final int port;
    private String state;
    private FireEvent currentFire;
    private boolean fireComplete;

    //Drone event constructor
    public DroneStatus(int droneId, int port, String state, FireEvent currentFire) {
        this.droneId = droneId;
        this.port = port;
        this.state = state;
        this.currentFire = currentFire;
        this.fireComplete = false;
    }

    /**
     * @return the drone's id
     */
    public int getDroneID() { return droneId; }

    /**
     * @return the drone's port
     */
    public int getPort() { return port; }

    /**
     * @return the drone's state
     */
    public String getState() { return state; }

    /**
     * @return the current fire event
     */
    public FireEvent getCurrentFire() { return currentFire; }

    /**
     * Sets the current state of the drone.
     *
     * @param state the drone's current state
     */
    public void setState(String state) { this.state = state; }

    /**
     * Set's the current fire vent of the drone
     *
     * @param currentFire the fire event assigned to the drone
     */
    public void setCurrentFire(FireEvent currentFire) { this.currentFire = currentFire; }

    /**
     * @return true if the fire is complete, and false otherwise
     */
    public boolean isFireComplete(){return this.fireComplete;}

    /**
     * @return String representation of a drone
     */
    @Override
    public String toString(){
        return "[ID:" + this.droneId + "][PORT:" + this.port + "][STATE:" + this.state +"]";
    }
}