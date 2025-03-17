import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class DroneSubsystemTest {

    private DroneSubsystem droneSubsystem;
    private TestScheduler testScheduler;
    private File tempFile;
    private FireIncidentSubsystem fireIncidentSubsystem;

    @BeforeEach
    public void setUp() throws IOException {

        testScheduler = new TestScheduler();

        // Create a temporary CSV file with some test data
        tempFile = File.createTempFile("fire_events", ".csv");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("10:00,1,FIRE_DETECTED,High\n");
            writer.write("10:05,2,FIRE_DETECTED,Moderate\n");
        }

        fireIncidentSubsystem = new FireIncidentSubsystem(tempFile.getAbsolutePath());
        droneSubsystem = new DroneSubsystem(100); // Initialize drone subsystem
    }

    @Test
    public void testFetchFireTask() throws InterruptedException {
        // Start the DroneSubsystem thread
        Thread droneThread = new Thread(droneSubsystem);
        droneThread.start();

        // Start the FireIncidentSubsystem thread
        Thread fireIncidentThread = new Thread(fireIncidentSubsystem);
        fireIncidentThread.start();

        // Wait for both threads to finish processing
        fireIncidentThread.join(5000); // Wait for up to 5 seconds for the fire incident thread to finish
        droneThread.join(5000); // Wait for up to 5 seconds for the drone thread to finish

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
    public void testCompleteTask() throws InterruptedException {
        // Start the DroneSubsystem thread
        Thread droneThread = new Thread(droneSubsystem);
        droneThread.start();

        // Start the FireIncidentSubsystem thread
        Thread fireIncidentThread = new Thread(fireIncidentSubsystem);
        fireIncidentThread.start();

        // Wait for both threads to finish processing
        fireIncidentThread.join(5000); // Wait for up to 5 seconds for the fire incident thread to finish
        droneThread.join(5000); // Wait for up to 5 seconds for the drone thread to finish
        FireEvent fireEvent = new FireEvent(1, "10:00", 1, "FIRE_DETECTED", "High");
        //droneSubsystem.completeTask(fireEvent);

        assertEquals(0, fireEvent.getRemainingWaterNeeded());
        assertTrue(droneSubsystem.isAgentEmpty());
    }
}