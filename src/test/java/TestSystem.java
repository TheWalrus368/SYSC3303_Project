import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestSystem {

    private static ModifiedDroneSubsystem drone;
    private static ModifiedScheduler modScheduler;
    private static ModifiedFireIncidentSubsystem fireIncident;
    private static Zone zoneA;

    private static Thread droneThread;
    private static Thread schedulerThread;
    private static Thread fireIncidentThread;


    @BeforeAll
    static void setup() throws Exception {

        // Init Fire Subsystem
        String csvFilePath = "src/main/java/fire_events.csv";
        fireIncident = new ModifiedFireIncidentSubsystem(csvFilePath);
        fireIncidentThread = new Thread(fireIncident, "FIRE");

        // Init DroneSubsystem
        drone = new ModifiedDroneSubsystem(100);
        droneThread = new Thread(drone);

        // Init Scheduler
        modScheduler = new ModifiedScheduler();
        schedulerThread = new Thread(modScheduler);

        zoneA = new Zone(1,0,0,10,10);
        //drone.currentZone = zoneA;
    }

    @Test
    void test() {
        assertTrue(true);
    }

    @Test
    void testFetchFireTask() throws InterruptedException{
        droneThread.start();
        fireIncidentThread.start();

        // wait for both threads to finish processing
        droneThread.join(5000); // wait up to 5 seconds for threads to finish
        fireIncidentThread.join(5000);

        // Simulate a fire event being sent to the drone
        String fireData = "NEW FIRE: FireEvent{'ID=1', time='10:00', zoneId=1, eventType='FIRE_DETECTED', severity='High', state='ACTIVE', failure='FAULT'}";
        FireEvent fireEvent = drone.parseDataToFireEvent(fireData);

        assertNotNull(fireEvent);
        assertEquals(1, fireEvent.getFireID());
        assertEquals("10:00", fireEvent.getTime());
        assertEquals(1, fireEvent.getZoneId());
        assertEquals("FIRE_DETECTED", fireEvent.getEventType());
        assertEquals("High", fireEvent.getSeverity());
        assertTrue(fireEvent.getFailureFlag());

    }

    @Test
    void testGetAgentLevel(){
        // Agent level should be at max capacity from the start
        assertEquals(15, drone.getAgentLevel());

        drone.dropAgent(5);
        assertEquals(10, drone.getAgentLevel());

        drone.refillAgent();
        assertEquals(15, drone.getAgentLevel());
    }

}
