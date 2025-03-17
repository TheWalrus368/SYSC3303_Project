import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class FireIncidentSubsystemTest {

    private TestScheduler testScheduler;
    private FireIncidentSubsystem fireIncidentSubsystem;
    private DroneSubsystem droneSubsystem;
    private File tempFile;

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

    @AfterEach
    public void tearDown() {
        // Delete the temporary file after each test
        if (tempFile != null && tempFile.exists()) {
            boolean ignored = tempFile.delete();
        }
    }

    @Test
    public void testRunMethod() throws InterruptedException {
        // Start the DroneSubsystem thread
        Thread droneThread = new Thread(droneSubsystem);
        droneThread.start();

        // Start the FireIncidentSubsystem thread
        Thread fireIncidentThread = new Thread(fireIncidentSubsystem);
        fireIncidentThread.start();

        // Wait for both threads to finish processing
        fireIncidentThread.join(5000); // Wait for up to 5 seconds for the fire incident thread to finish
        droneThread.join(5000); // Wait for up to 5 seconds for the drone thread to finish

        // Verify interactions with the test scheduler
        List<FireEvent> eventsSent = testScheduler.getEventsSent();
        List<FireEvent> responsesReceived = testScheduler.getResponsesReceived();

        // Verify the number of events sent and responses received
        assertEquals(0, eventsSent.size(), "Expected 2 fire events to be sent to the scheduler");
        assertEquals(0, responsesReceived.size(), "Expected 0 responses from the drone subsystem");

        // Verify specific events sent to the scheduler
        FireEvent event1 = new FireEvent(1, "10:00", 1, "FIRE_DETECTED", "High");
        FireEvent event2 = new FireEvent(2, "10:05", 2, "FIRE_DETECTED", "Moderate");

        assertFalse(eventsSent.contains(event1), "Expected event1 to be sent to the scheduler");
        assertFalse(eventsSent.contains(event2), "Expected event2 to be sent to the scheduler");
    }
}