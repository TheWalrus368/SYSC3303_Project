import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Tony Situ
 * @version 30/01/2025
 * The {@code DroneSubsystemTest} class contains unit tests for the {@code DroneSubsystem}.
 * It verifies the correct execution of the drone's behavior when processing fire events.
 */
public class DroneSubsystemTest {

    private TestScheduler testScheduler;
    private DroneSubsystem droneSubsystem;

    /**
     * Sets up the test environment before each test case.
     * Initializes a {@code TestScheduler} and a {@code DroneSubsystem} instance.
     */
    @BeforeEach
    public void setUp() {
        testScheduler = new TestScheduler();
        droneSubsystem = new DroneSubsystem(testScheduler, 100);
    }

    /**
     * Tests the {@code run} method of {@code DroneSubsystem}.
     * It ensures that the drone correctly fetches a fire event from the scheduler
     * and processes it as expected.
     *
     * @throws InterruptedException If the thread execution is interrupted.
     */
    @Test
    public void testRunMethod() throws InterruptedException {
        // Preload a task into the scheduler
        FireEvent mockEvent = new FireEvent("10:00", 1, "FIRE_DETECTED", "High");
        testScheduler.receiveFireEvent(mockEvent);

        // Start the thread
        Thread thread = new Thread(droneSubsystem);
        thread.start();

        // Wait for some time to let the thread process
        thread.join(1000);

        // Verify interactions with the test scheduler
        assertEquals(mockEvent, testScheduler.receivedTaskDrone);
    }
}