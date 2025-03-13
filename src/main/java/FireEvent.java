/**
 * Represents a fire event occurring in a specific zone.
 * This event includes details such as the time of occurrence,
 * zone ID, event type, severity level, and the amount of water needed to extinguish the fire.
 */
public class FireEvent {
    private final String time;       // Event timestamp
    private final int zoneId;        // Fire zone ID
    private final String eventType;  // Type of event (FIRE_DETECTED or DRONE_REQUEST)
    private final String severity;   // Severity level (High, Moderate, Low)
    private int remainingWaterNeeded; 
    private int fireID;
    private static int nextFireID = 1;

    /**
     * Constructs a new FireEvent instance.
     *
     * @param time     The timestamp of the fire event.
     * @param zoneId   The ID of the zone where the fire occurred.
     * @param eventType The type of event (e.g., FIRE_DETECTED, DRONE_REQUEST).
     * @param severity  The severity of the fire (High, Moderate, Low).
     */
    public FireEvent(String time, int zoneId, String eventType, String severity) {
        this.time = time;
        this.zoneId = zoneId;
        this.eventType = eventType;
        this.severity = severity;
        this.fireID = nextFireID;
        nextFireID ++;
        this.remainingWaterNeeded = getWaterRequired();
    }

    /**
     * Gets the timestamp of the fire event.
     *
     * @return The event timestamp.
     */
    public String getTime() {
        return time;
    }

    /**
     * Gets the ID of the fire zone.
     *
     * @return The zone ID where the fire occurred.
     */
    public int getZoneId() {
        return zoneId;
    }

     /**
     * Gets the type of fire event.
     *
     * @return The event type (FIRE_DETECTED or DRONE_REQUEST).
     */
    public String getEventType() {
        return eventType;
    }

    /**
     * Gets the severity level of the fire.
     *
     * @return The severity level (High, Moderate, Low).
     */
    public String getSeverity() {
        return severity;
    }

    /**
     * Gets the unique fire ID.
     *
     * @return The fire event's unique identifier.
     */
    public int getFireID(){
        return fireID;
    }

    /**
     * Reduces the remaining water needed to extinguish the fire.
     *
     * @param agentAmount The amount of extinguishing agent used.
     */
    public void extinguish(int agentAmount){
        this.remainingWaterNeeded -= agentAmount;
    }

    /**
     * Gets the remaining water needed to put out the fire.
     *
     * @return The amount of water still required.
     */
    public int getRemainingWaterNeeded(){
        return remainingWaterNeeded;
    }

    /**
     * Determines the water required based on fire severity.
     *
     * @return The amount of water required to extinguish the fire.
     */
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

    /**
     * Provides a string representation of the fire event.
     *
     * @return A string containing fire event details.
     */
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
