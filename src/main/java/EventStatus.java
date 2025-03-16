class EventStatus {
    private int eventID;
    private int port;
    private String state;
    private FireEvent currentFire;

    //Drone event constructor
    public EventStatus(int eventID, int port, String state, FireEvent currentFire) {
        this.eventID = eventID;
        this.port = port;
        this.state = state;
        this.currentFire = currentFire;
    }

    //Fire event constructor
    public EventStatus(String state){
        this.state = state;
    }

    public int getEventID() { return eventID; }
    public int getPort() { return port; }
    public String getState() { return state; }
    public FireEvent getCurrentFire() { return currentFire; }

    public void setState(String state) { this.state = state; }
    public void setCurrentFire(FireEvent currentFire) { this.currentFire = currentFire; }
}