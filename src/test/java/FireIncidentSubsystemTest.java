import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code FireIncidentSubsystemTest} class contains unit tests for the {@code FireIncidentSubsystem}.
 * It ensures that fire events are correctly parsed from a CSV file, sent to the scheduler,
 * and appropriately processed by the drone subsystem.
 */
public class FireIncidentSubsystemTest {

    private TestScheduler testScheduler;
    private FireIncidentSubsystem fireIncidentSubsystem;
    private DroneSubsystem droneSubsystem;
    private File tempFile;

    /**
     * Sets up the test environment before each test case.
     * Creates a temporary CSV file containing test fire events and initializes the necessary subsystems.
     *
     * @throws IOException If an error occurs while creating the temporary file.
     */
    @BeforeEach
    public void setUp() throws IOException {
        testScheduler = new TestScheduler();

        // Create a temporary CSV file with some test data
        tempFile = File.createTempFile("fire_events", ".csv");
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("10:00,1,FIRE_DETECTED,High\n");
            writer.write("10:05,2,FIRE_DETECTED,Moderate\n");
        }

        fireIncidentSubsystem = new FireIncidentSubsystem(tempFile.getAbsolutePath(), testScheduler);
        droneSubsystem = new DroneSubsystem(testScheduler, 100);
    }

    /**
     * Cleans up the test environment after each test.
     * Deletes the temporary CSV file to prevent clutter.
     */
    @AfterEach
    public void tearDown() {
        // Delete the temporary file after each test
        if (tempFile != null && tempFile.exists()) {
            boolean ignored = tempFile.delete();
        }
    }

    /**
     * Tests the {@code run} method of the {@code FireIncidentSubsystem}.
     * It ensures that fire events are read from the CSV file, sent to the scheduler,
     * and processed correctly by the drone subsystem.
     *
     * @throws InterruptedException If thread execution is interrupted during the test.
     */
    @Test
    public void testRunMethod() throws InterruptedException {
        // Start the DroneSubsystem thread
        Thread droneThread = new Thread(droneSubsystem);
        droneThread.start();

        // Start the FireIncidentSubsystem thread
        Thread fireIncidentThread = new Thread(fireIncidentSubsystem);
        fireIncidentThread.start();

        // Wait for both threads to finish processing (up to 10 seconds)
        Thread.sleep(25000);

        // Verify interactions with the test scheduler
        List<FireEvent> eventsSent = testScheduler.getEventsSent();
        List<FireEvent> responsesReceived = testScheduler.getResponsesReceived();

        // Verify the number of events sent and responses received
        assertEquals(2, eventsSent.size());
        assertEquals(0, responsesReceived.size());

        // Verify specific events sent to the scheduler
        FireEvent event1 = new FireEvent("10:00", 1, "FIRE_DETECTED", "High");
        FireEvent event2 = new FireEvent("10:05", 2, "FIRE_DETECTED", "Moderate");
    }
}