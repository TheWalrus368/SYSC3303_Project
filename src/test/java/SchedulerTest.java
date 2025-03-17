import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SchedulerTest {

    private Scheduler scheduler;

    @BeforeEach
    public void setUp() {
        // Use a unique port for each test to avoid conflicts
        scheduler = new Scheduler("src/main/java/sample_zone.csv") {

            public void receiveFireEvent(FireEvent event) {
            }

            public FireEvent assignTaskToDrone() {
                return null;
            }
        };
    }

    @AfterEach
    public void tearDown() {
        // Close the DatagramSocket to release the port

    }

    @Test
    public void testHandleEvent() {
        String data = "[DRONE: 100][PORT: 6100][STATE: READY]";
        EventStatus eventStatus = scheduler.handleEvent(data);

        assertEquals("READY", eventStatus.getCommand());
        assertNull(eventStatus.getDroneStatus());

        data = "NEW FIRE: FireEvent{'ID=1', time='10:00', zoneId=1, eventType='FIRE_DETECTED', severity='High', state='ACTIVE'}";
        eventStatus = scheduler.handleEvent(data);
        assertEquals("FIRE", eventStatus.getCommand());

        data = "REQUEST CONFIRMATION: FireEvent{'ID=1', time='10:00', zoneId=1, eventType='FIRE_DETECTED', severity='High', state='ACTIVE'}";
        eventStatus = scheduler.handleEvent(data);
        assertEquals("CONFIRMATION", eventStatus.getCommand());
    }
}