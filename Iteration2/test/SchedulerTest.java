import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * The {@code SchedulerTest} class contains unit tests for the {@code Scheduler} class.
 * It verifies that fire events are correctly assigned, received, and processed by the scheduler.
 */
public class SchedulerTest {

    private TestScheduler scheduler;

    /**
     * Sets up the test environment before each test case.
     * Initializes a new {@code Scheduler} instance.
     */
    @BeforeEach
    public void setUp() {
        scheduler = new TestScheduler();
    }

    /**
     * Tests the {@code receiveFireEvent} method to ensure that a fire event is correctly
     * received and stored in the scheduler for processing by the drone subsystem.
     *
     */
    @Test
    public void testReceiveFireEvent() {
        FireEvent event = new FireEvent("10:00", 1, "FIRE_DETECTED", "High");

        // Mocking behavior is not necessary here since we're testing direct method calls
        scheduler.receiveFireEvent(event);

        synchronized (scheduler) {
            assertTrue(scheduler.taskReady);
            assertEquals(event, scheduler.taskForDrone);
        }
    }

    /**
     * Tests the {@code returnFireEvent} method to ensure that a fire event response
     * is correctly received from the drone subsystem and stored in the scheduler.
     *
     */
    @Test
    public void testReturnFireEvent() {
        FireEvent event = new FireEvent("10:00", 1, "FIRE_DETECTED", "High");

        // Set up initial state
        scheduler.taskResponseReceived = false;

        scheduler.sendDroneAcknowledgment(event);

        synchronized (scheduler) {
            assertTrue(scheduler.taskResponseReceived);
            assertEquals(event, scheduler.receivedTaskDrone);
        }
    }

    /**
     * Tests the {@code assignTaskToDrone} method to ensure that a fire event
     * is correctly assigned to the drone subsystem and marked as processed.
     *
     */
    @Test
    public void testAssignTaskToDrone() {
        FireEvent event = new FireEvent("10:00", 1, "FIRE_DETECTED", "High");
        scheduler.taskForDrone = event;
        scheduler.taskReady = true;

        FireEvent assignedTask = scheduler.assignTaskToDrone();

        synchronized (scheduler) {
            assertFalse(scheduler.taskReady);
            assertEquals(event, assignedTask);
        }
    }

    /**
     * Tests the {@code returnResponseToFire} method to ensure that a fire event response
     * is correctly returned to the fire incident subsystem and marked as processed.
     *
     */
    @Test
    public void testReturnResponseToFire() {
        FireEvent event = new FireEvent("10:00", 1, "FIRE_DETECTED", "High");
        scheduler.receivedTaskDrone = event;
        scheduler.taskResponseReceived = true;

        FireEvent response = scheduler.receiveDroneAcknowledgment();

        synchronized (scheduler) {
            assertFalse(scheduler.taskResponseReceived);
            assertEquals(event, response);
        }
    }
}