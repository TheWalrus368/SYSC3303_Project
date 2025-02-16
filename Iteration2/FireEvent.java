public class FireEvent {
    private final String time;       // Event timestamp
    private final int zoneId;        // Fire zone ID
    private final String eventType;  // Type of event (FIRE_DETECTED or DRONE_REQUEST)
    private final String severity;   // Severity level (High, Moderate, Low)
    private int remainingWaterNeeded; 
    private int fireID;
    private static int nextFireID = 1;

    public FireEvent(String time, int zoneId, String eventType, String severity) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.fireID = nextFireID;
        nextFireID ++;
        this.remainingWaterNeeded = getWaterRequired();
    }

    public String getTime() {
        return time;
    }

    public int getZoneId() {
        return zoneId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getSeverity() {
        return severity;
    }

    public int getFireID(){
        return fireID;
    }

    public void extinguish(int agentAmount){
        this.remainingWaterNeeded -= agentAmount;
    }

    public int getRemainingWaterNeeded(){
        return remainingWaterNeeded;
    }

    public int getWaterRequired() {
        switch (severity.toUpperCase()){
            case "HIGH":
            return 30; // High severity needs 30L

            case "MODERATE":
            return 20; // Moderate severity needs 20L

            case "LOW":
            return 10; // Low severity needs 10L

            default:
            return 0;
        }
    }

    @Override
    public String toString() {
        return "FireEvent{" +
                "time='" + time + '\'' +
                ", zoneId=" + zoneId +
                ", eventType='" + eventType + '\'' +
                ", severity='" + severity + '\'' +
                '}';
    }
}
