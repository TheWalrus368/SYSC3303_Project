import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class DroneSubsystemTest {

    private DroneSubsystem droneSubsystem;

    @BeforeEach
    public void setUp() {
        droneSubsystem = new DroneSubsystem(100);
    }

    @Test
    public void testFetchFireTask() {
        // Simulate a fire event being sent to the drone
        String fireData = "NEW FIRE: FireEvent{'ID=1', time='10:00', zoneId=1, eventType='FIRE_DETECTED', severity='High', state='ACTIVE'}";
        FireEvent fireEvent = droneSubsystem.parseDataToFireEvent(fireData);

        assertNotNull(fireEvent);
        assertEquals(1, fireEvent.getFireID());
        assertEquals("10:00", fireEvent.getTime());
        assertEquals(1, fireEvent.getZoneId());
        assertEquals("FIRE_DETECTED", fireEvent.getEventType());
        assertEquals("High", fireEvent.getSeverity());
    }

    @Test
    public void testCompleteTask() {
        FireEvent fireEvent = new FireEvent(1, "10:00", 1, "FIRE_DETECTED", "High");
        droneSubsystem.completeTask(fireEvent);

        assertEquals(0, fireEvent.getRemainingWaterNeeded());
        assertTrue(droneSubsystem.isAgentEmpty());
    }
}