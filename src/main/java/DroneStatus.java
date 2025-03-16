class DroneStatus {
    private int droneId;
    private int port;
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

    public int getDroneID() { return droneId; }
    public int getPort() { return port; }
    public String getState() { return state; }
    public FireEvent getCurrentFire() { return currentFire; }

    public void setState(String state) { this.state = state; }
    public void setCurrentFire(FireEvent currentFire) { this.currentFire = currentFire; }

    public boolean isFireComplete(){return this.fireComplete;}
}