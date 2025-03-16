public class EventStatus {

    private String command;
    private DroneStatus droneStatus = null;

    public EventStatus(String command){
        this.command = command;
    }

    public EventStatus(String command, DroneStatus droneStatus){
        this.command = command;
        this.droneStatus = droneStatus;
    }

    public String getCommand(){
        return this.command;
    }

    public DroneStatus getDroneStatus() {
        return this.droneStatus;
    }
}
