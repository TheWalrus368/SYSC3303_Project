import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class TestSystem {

    private static ModifiedDroneSubsystem drone;
    private static ModifiedScheduler modScheduler;
    private static ModifiedFireIncidentSubsystem fireIncident;
    private static Zone zoneA;
    private static List<DroneStatus> drones;

    @BeforeAll
    static void setup() throws Exception {

        // Init Fire Subsystem
        String csvFilePath = "src/main/java/fire_events.csv";
        fireIncident = new ModifiedFireIncidentSubsystem(csvFilePath);

        // Init DroneSubsystem
        drone = new ModifiedDroneSubsystem(100);

        // Init Scheduler
        modScheduler = new ModifiedScheduler();

        zoneA = new Zone(1,0,0,10,10);

        drones = new ArrayList<>();

    }

    // DRONE SUBSYSTEM TESTS
    @Test
    void testFetchFireTask() throws InterruptedException{
        Thread droneThread = new Thread();
        Thread fireIncidentThread = new Thread();

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

    @Test
    void testSimulateDroneTravel(){
        assertEquals(drone.BASE_ZONE, drone.getCurrentZone());

        drone.simulateDroneTravel(zoneA);

        assertEquals(zoneA, drone.getCurrentZone());

        double expectedX = (zoneA.getStartX() + zoneA.getEndX());
        double expectedY = (zoneA.getStartY() + zoneA.getEndY());
        assertEquals(expectedX, drone.droneX, 0.0001);
        assertEquals(expectedY, drone.droneY, 0.0001);
    }

    @Test
    void testCurrentFireExtinguished(){

        FireEvent fireEvent = new FireEvent(1, "12:00", 101, "IDLE", "Moderate", "None");
        drone.setCurrentFireEvent(fireEvent);

        assertFalse(drone.currentFireExtinguished(), "Fire should not be extinguished yet");

        // simulate extinguishing fire
        int waterToDrop = 20;
        drone.dropAgent(waterToDrop);
        fireEvent.extinguish(waterToDrop);
        assertTrue(drone.currentFireExtinguished());
    }

    // SCHEDULER TESTS
    @Test
    void testExtractFireEvent(){
        String input = "[Scheduler <- Drone] FAULT: FireEvent{'ID=123', time='2025-03-28 14:00:00', zoneId=5, eventType='DRONE_REQUEST', severity='High', state='Active', failure='FAULT'}";
        String expected = "FireEvent{'ID=123', time='2025-03-28 14:00:00', zoneId=5, eventType='DRONE_REQUEST', severity='High', state='Active', failure='FAULT'}";

        String result = modScheduler.extractFireEvent(input);

        assertEquals(expected, result);
    }

    @Test
    void testGetAvailableDrone(){
        DroneStatus idleDrone = new DroneStatus(1, 101, "IDLE", null);
        DroneStatus usedDrone = new DroneStatus(2, 102, "USED", null);

        modScheduler.getDrones().add(idleDrone);
        modScheduler.getDrones().add(usedDrone);

        DroneStatus availableDrone = modScheduler.getAvailableDrone();

        assertEquals(idleDrone, availableDrone, "Returned drone should be idle drone");

        assertNotNull(availableDrone, "A drone should be available");
        assertEquals("USED", idleDrone.getState(), "State of idle drone should be changed to USED");
    }

    @Test
    void testHandleEvent(){
        String event = "[DRONE: 101][PORT: 1][STATE: IDLE]";
        EventStatus status = modScheduler.handleEvent(event);

        // verify new drone event returns IDLE
        assertEquals("IDLE", status.getCommand());

        DroneStatus drone = status.getDroneStatus();

        // verify that the drone has been created
        assertNotNull(drone);
        assertEquals(101, drone.getDroneID());
        assertEquals("IDLE", drone.getState());
    }

    @Test
    void testAddSortFires(){
        String fire1 = "FireEvent{'ID=1', time='12:00', zoneId=1, eventType='FIRE_DETECTED', severity='High', state='Active', failure='Fault'}";
        String fire2 = "FireEvent{'ID=2', time='12:05', zoneId=2, eventType='FIRE_DETECTED', severity='Low', state='Active', failure='None'}";
        String fire3 = "FireEvent{'ID=3', time='12:10', zoneId=3, eventType='FIRE_DETECTED', severity='Moderate', state='Active', failure='None'}";

        modScheduler.addSortFires(fire1);
        modScheduler.addSortFires(fire2);
        modScheduler.addSortFires(fire3);

        assertEquals(fire1, modScheduler.fireToDroneBuffer.removeFirst()); // should be "High"
        assertEquals(fire3, modScheduler.fireToDroneBuffer.removeFirst()); // should be "Moderate"
        assertEquals(fire2, modScheduler.fireToDroneBuffer.removeFirst()); // should be "Low"

    }


    // FIRE INCIDENT SUBSYSTEM TESTS
    @Test
    void testRunMethod() throws InterruptedException {
        Thread droneThread = new Thread();
        Thread fireIncidentThread = new Thread();

        droneThread.start();
        fireIncidentThread.start();

        // Wait for both threads to finish processing
        fireIncidentThread.join(5000); // Wait for up to 5 seconds for the fire incident thread to finish
        droneThread.join(5000); // Wait for up to 5 seconds for the drone thread to finish

        // Verify interactions with the test scheduler
        List<FireEvent> eventsSent = modScheduler.getEventsSent();
        List<FireEvent> responsesReceived = modScheduler.getResponsesReceived();

        // Verify the number of events sent and responses received
        assertEquals(0, eventsSent.size(), "Expected 2 fire events to be sent to the scheduler");
        assertEquals(0, responsesReceived.size(), "Expected 0 responses from the drone subsystem");

        // Verify specific events sent to the scheduler
        FireEvent event1 = new FireEvent(1, "10:00", 1, "FIRE_DETECTED", "High", "FAULT");
        FireEvent event2 = new FireEvent(2, "10:05", 2, "FIRE_DETECTED", "Moderate", "None");

        assertFalse(eventsSent.contains(event1), "Expected event1 to be sent to the scheduler");
        assertFalse(eventsSent.contains(event2), "Expected event2 to be sent to the scheduler");
    }

    @Test
    void testExtractFireEventFromLine(){
        String line = "12:00,1,FIRE_DETECTED,High,Fault";
        FireEvent event = fireIncident.extractFireEventFromLine(line);

        assertNotNull(event);
        assertEquals(1, event.getFireID()); // First event should have ID 1
        assertEquals("12:00", event.getTime());
        assertEquals(1, event.getZoneId());
        assertEquals("FIRE_DETECTED", event.getEventType());
        assertEquals("High", event.getSeverity());
    }

}
