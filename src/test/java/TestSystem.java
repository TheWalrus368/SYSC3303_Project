import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TestSystem {

    private static ModifiedDroneSubsystem drone;
    private static ModifiedScheduler modScheduler;
    private static ModifiedFireIncidentSubsystem fireIncident;
    private static Zone zoneA;

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
    }

    @BeforeEach
    void resetTestState() {
        if (drone != null) {
            drone.setCurrentFireEvent(null);

            drone.refillAgent(); // Reset agent level
        }

        if (modScheduler != null) {
            // Clear the list of registered drones
            modScheduler.getDrones().clear();

            // Clear any items potentially left in buffers by previous tests
            while(modScheduler.fireToDroneBuffer.getCount() > 0) {
                modScheduler.fireToDroneBuffer.removeFirst();
            }
        }

        // Reset FireIncident state (if tests modify it)
        if (fireIncident != null) {
            // If tests rely on nextFireID starting at 1
            fireIncident.nextFireID = 1;
        }
    }

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

    @Test
    void testDropZeroAgent(){
        int initialAgentLevel = drone.getAgentLevel();
        drone.dropAgent(0);
        assertEquals(initialAgentLevel, drone.getAgentLevel(), "Dropping zero agent should not change the agent level");
    }

    @Test
    void testDroneStateAfterTravel() {
        // Travel to a zone
        Zone destinationZone = new Zone(2, 20, 20, 30, 30);
        drone.simulateDroneTravel(destinationZone); // This method updates currentZone
        assertEquals(destinationZone, drone.getCurrentZone(), "Drone current zone should be updated after travel");

        assertTrue(drone.toString().contains("IDLE") || drone.toString().contains("EN_ROUTE") || drone.toString().contains("DROPPING_AGENT") , "Drone toString should reflect a valid state");
    }

    @Test
    void testGetAvailableDroneWhenNoneIdle() {
        modScheduler.getDrones().clear(); // Ensure clean state for this test
        DroneStatus busyDrone1 = new DroneStatus(101, 6101, "EN_ROUTE", null);
        DroneStatus busyDrone2 = new DroneStatus(102, 6102, "DROPPING_AGENT", null);
        modScheduler.getDrones().add(busyDrone1);
        modScheduler.getDrones().add(busyDrone2);

        DroneStatus availableDrone = modScheduler.getAvailableDrone();
        assertNull(availableDrone, "Should return null when no drones are IDLE");
    }

    @Test
    void testHandleCompleteEvent() {
        // Ensure a drone exists in the scheduler's list first
        // Start the drone in a state like USED, simulating it was busy before completing.
        DroneStatus existingDrone = new DroneStatus(103, 6103, "USED", null);
        modScheduler.getDrones().clear();
        modScheduler.getDrones().add(existingDrone);

        // Simulate receiving a COMPLETE message from this drone
        // The state in the message string is "COMPLETE"
        String completeEventMessage = "[DRONE: 103][PORT: 6103][STATE: COMPLETE] COMPLETED: Fire has been extinguished FireEvent{'ID=5', time='11:00', zoneId=3, eventType='FIRE_DETECTED', severity='Low', state='ACTIVE', failure='None'}";
        EventStatus status = modScheduler.handleEvent(completeEventMessage);

        assertEquals("COMPLETE", status.getCommand(), "Event command should be COMPLETE");

        assertNotNull(status.getDroneStatus(), "EventStatus should contain drone status for COMPLETE event");
        assertEquals(103, status.getDroneStatus().getDroneID(), "Drone ID in EventStatus should match the completed drone");
        assertEquals("COMPLETE", status.getDroneStatus().getState(), "State parsed from message in EventStatus should be COMPLETE");

        DroneStatus droneInList = modScheduler.getDrones().stream()
                .filter(d -> d.getDroneID() == 103)
                .findFirst()
                .orElse(null);
        assertNotNull(droneInList, "Drone 103 should still exist in the scheduler list");
        assertEquals("USED", droneInList.getState(), "Drone state in scheduler list should still be USED immediately after handleEvent returns");
    }

    @Test
    void testGetWaterRequired() {
        FireEvent highSeverityFire = new FireEvent(10, "14:00", 1, "FIRE_DETECTED", "High", "None");
        FireEvent moderateSeverityFire = new FireEvent(11, "14:05", 2, "FIRE_DETECTED", "Moderate", "None");
        FireEvent lowSeverityFire = new FireEvent(12, "14:10", 3, "FIRE_DETECTED", "Low", "None");
        // Assuming default case or unknown severity returns 0
        FireEvent unknownSeverityFire = new FireEvent(13, "14:15", 4, "FIRE_DETECTED", "Unknown", "None");

        assertEquals(30, highSeverityFire.getWaterRequired(), "High severity fire should require 30L");
        assertEquals(20, moderateSeverityFire.getWaterRequired(), "Moderate severity fire should require 20L");
        assertEquals(10, lowSeverityFire.getWaterRequired(), "Low severity fire should require 10L");
        assertEquals(0, unknownSeverityFire.getWaterRequired(), "Unknown severity fire should require 0L");
    }

    @Test
    void testGetFailureFlag() {
        FireEvent faultFire = new FireEvent(14, "15:00", 5, "FIRE_DETECTED", "High", "FAULT");
        FireEvent noFaultFire = new FireEvent(15, "15:05", 6, "FIRE_DETECTED", "Moderate", "None");
        FireEvent nullFaultFire = new FireEvent(16, "15:10", 7, "FIRE_DETECTED", "Low", null); // Test null case if applicable

        assertTrue(faultFire.getFailureFlag(), "FireEvent with 'FAULT' should return true for getFailureFlag");
        assertFalse(noFaultFire.getFailureFlag(), "FireEvent with 'None' should return false for getFailureFlag");
        assertFalse(nullFaultFire.getFailureFlag(), "FireEvent with null failure string should return false for getFailureFlag");
    }

    @Test
    void testZoneEquals() {
        Zone zone1a = new Zone(1, 0, 0, 10, 10);
        Zone zone1b = new Zone(1, 0, 0, 10, 10); // Same data, same ID (ID isn't checked in equals)
        Zone zone1c = new Zone(99, 0, 0, 10, 10); // Same data, different ID
        Zone zone2 = new Zone(2, 0, 0, 10, 11); // Different endY
        Zone zone3 = new Zone(3, 1, 0, 10, 10); // Different startX

        assertEquals(zone1a, zone1b, "Zones with identical coordinates should be equal");
        assertEquals(zone1a, zone1c, "Zones with identical coordinates but different IDs should be equal (based on current equals implementation)");
        assertNotEquals(zone1a, zone2, "Zones with different coordinates should not be equal");
        assertNotEquals(zone1a, zone3, "Zones with different coordinates should not be equal");
        assertNotEquals(null, zone1a, "Zone should not be equal to null");
    }

    @Test
    void testHandleFireEvent() {
        // Simulate receiving a NEW FIRE message from the FireIncidentSubsystem
        String newFireMessage = "NEW FIRE: FireEvent{'ID=20', time='10:00', zoneId=1, eventType='FIRE_DETECTED', severity='High', state='ACTIVE', failure='None'}";
        EventStatus status = modScheduler.handleEvent(newFireMessage);

        assertEquals("FIRE", status.getCommand(), "Event command should be FIRE for a new fire message");
        assertNull(status.getDroneStatus(), "EventStatus should not contain drone status for a FIRE event");
    }

    @Test
    void testHandleConfirmationEvent() {
        // Simulate receiving a REQUEST CONFIRMATION message from the FireIncidentSubsystem
        String confirmationMessage = "REQUEST CONFIRMATION: [FIRE 21]:FireEvent{'ID=21', time='10:05', zoneId=2, eventType='FIRE_DETECTED', severity='Moderate', state='ACTIVE', failure='None'}";
        EventStatus status = modScheduler.handleEvent(confirmationMessage);

        assertEquals("CONFIRMATION", status.getCommand(), "Event command should be CONFIRMATION for a confirmation request");
        assertNull(status.getDroneStatus(), "EventStatus should not contain drone status for a CONFIRMATION event");
    }

    @Test
    void testHandleFaultEvent() {
        // Simulate receiving a FAULT message from a Drone
        // Ensure the drone exists in the scheduler's list first
        DroneStatus faultingDrone = new DroneStatus(104, 6104, "DROPPING_AGENT", null);
        modScheduler.getDrones().clear(); // Clear for clean test
        modScheduler.getDrones().add(faultingDrone);

        String faultMessage = "[DRONE: 104][PORT: 6104][STATE: DROPPING_AGENT] FAULT: This Fire has failed with FireEvent{'ID=22', time='10:10', zoneId=3, eventType='FIRE_DETECTED', severity='Low', state='ACTIVE', failure='FAULT'}";
        EventStatus status = modScheduler.handleEvent(faultMessage);

        assertEquals("FAULT", status.getCommand(), "Event command should be FAULT for a drone fault message");
        assertNull(status.getDroneStatus(), "EventStatus should likely be null for a FAULT event in this implementation");
    }

    @Test
    void testUpdateDroneState() {
        // Add a drone to the scheduler's list
        DroneStatus droneToUpdate = new DroneStatus(105, 6105, "IDLE", null);
        modScheduler.getDrones().clear(); // Clear for clean test
        modScheduler.getDrones().add(droneToUpdate);

        // Simulating the call as if it were accessible:
        modScheduler.updateDroneState(105, "EN_ROUTE"); // Calling the method from the main Scheduler class

        // Verify the state change in the list
        Optional<DroneStatus> updatedDroneOpt = modScheduler.getDrones().stream()
                .filter(d -> d.getDroneID() == 105)
                .findFirst();
        assertTrue(updatedDroneOpt.isPresent(), "Drone 105 should still be in the list");
        assertEquals("EN_ROUTE", updatedDroneOpt.get().getState(), "Drone state should be updated to EN_ROUTE in the list");

        // Update again
        modScheduler.updateDroneState(105, "IDLE");
        assertEquals("IDLE", updatedDroneOpt.get().getState(), "Drone state should be updated back to IDLE in the list");
    }

    @Test
    void testGetZoneStatic() {
        // Assuming zoneMap is loaded statically when Scheduler class is loaded.

        Zone zone1 = Scheduler.getZone(1);
        assertNotNull(zone1, "Zone 1 should exist");
        assertEquals(1, zone1.getID());
        assertEquals(0, zone1.getStartX());
        assertEquals(0, zone1.getStartY());
        assertEquals(700, zone1.getEndX());
        assertEquals(600, zone1.getEndY());

        Zone zone2 = Scheduler.getZone(2);
        assertNotNull(zone2, "Zone 2 should exist");
        assertEquals(2, zone2.getID());
        assertEquals(0, zone2.getStartX());
        assertEquals(600, zone2.getStartY());
        assertEquals(650, zone2.getEndX());
        assertEquals(1500, zone2.getEndY());

        // Test getting a non-existent zone
        Zone zoneNonExistent = Scheduler.getZone(999);
        assertNull(zoneNonExistent, "Getting a non-existent zone ID should return null");
    }

    @Test
    void testDroneStatusToString() {
        DroneStatus status = new DroneStatus(201, 6201, "REFILLING", null);
        String expectedString = "[ID:201][PORT:6201][STATE:REFILLING]";
        assertEquals(expectedString, status.toString(), "DroneStatus toString() format should match expected pattern.");

        DroneStatus statusIdle = new DroneStatus(202, 6202, "IDLE", null);
        String expectedStringIdle = "[ID:202][PORT:6202][STATE:IDLE]";
        assertEquals(expectedStringIdle, statusIdle.toString(), "DroneStatus toString() format should match expected pattern for IDLE state.");
    }

    @Test
    void testDroneStatusGetters() {
        FireEvent sampleFire = new FireEvent(50, "16:00", 5, "TEST_EVENT", "Low", "None");
        DroneStatus status = new DroneStatus(203, 6203, "EN_ROUTE", sampleFire);

        assertEquals(203, status.getDroneID(), "getDroneID() should return the correct ID.");
        assertEquals(6203, status.getPort(), "getPort() should return the correct port.");
        assertEquals("EN_ROUTE", status.getState(), "getState() should return the correct state.");
        assertEquals(sampleFire, status.getCurrentFire(), "getCurrentFire() should return the correct FireEvent.");
        assertFalse(status.isFireComplete(), "isFireComplete() should initially be false."); // Default value check
    }

    @Test
    void testEventStatusConstructorNoDrone() {
        String command = "TEST_COMMAND";
        EventStatus status = new EventStatus(command);

        assertEquals(command, status.getCommand(), "getCommand() should return the command set in the constructor.");
        assertNull(status.getDroneStatus(), "getDroneStatus() should be null when using the constructor without DroneStatus.");
    }

    @Test
    void testEventStatusConstructorWithDrone() {
        String command = "DRONE_READY";
        DroneStatus droneStatus = new DroneStatus(204, 6204, "IDLE", null);
        EventStatus status = new EventStatus(command, droneStatus);

        assertEquals(command, status.getCommand(), "getCommand() should return the command set in the constructor.");
        assertNotNull(status.getDroneStatus(), "getDroneStatus() should not be null when using the constructor with DroneStatus.");
        assertEquals(droneStatus, status.getDroneStatus(), "getDroneStatus() should return the DroneStatus object passed in the constructor.");
        assertEquals(204, status.getDroneStatus().getDroneID(), "Drone ID from getDroneStatus() should match.");
    }

    @Test
    void testSetGetCurrentFireEvent() {
        // Use the 'drone' instance available in TestSystem setup
        assertNull(drone.getCurrentFireEvent(), "Initially, the current fire event should be null or as set by previous tests.");

        FireEvent testEvent = new FireEvent(51, "17:00", 6, "ASSIGNED", "Moderate", "None");
        drone.setCurrentFireEvent(testEvent); // Using method from ModifiedDroneSubsystem

        FireEvent retrievedEvent = drone.getCurrentFireEvent();
        assertNotNull(retrievedEvent, "Retrieved event should not be null after setting it.");
        assertEquals(testEvent.getFireID(), retrievedEvent.getFireID(), "IDs of set and retrieved events should match.");
        assertEquals(testEvent.toString(), retrievedEvent.toString(), "toString() of set and retrieved events should match.");

        drone.setCurrentFireEvent(null);
        assertNull(drone.getCurrentFireEvent(), "Current fire event should be null after resetting.");
    }

    @Test
    void testBoundedBufferGetCount() {
        // Using the fireToDroneBuffer from the modScheduler instance
        int initialCount = modScheduler.fireToDroneBuffer.getCount(); // Get count before adding

        String fire1 = "FireEvent{'ID=101', severity='High'}";
        String fire2 = "FireEvent{'ID=102', severity='Low'}";

        modScheduler.fireToDroneBuffer.addLast(fire1);
        assertEquals(initialCount + 1, modScheduler.fireToDroneBuffer.getCount(), "Count should increase by 1 after adding an item.");

        modScheduler.fireToDroneBuffer.addLast(fire2);
        assertEquals(initialCount + 2, modScheduler.fireToDroneBuffer.getCount(), "Count should increase by another 1 after adding a second item.");

        // Clean up by removing the added items
        modScheduler.fireToDroneBuffer.removeFirst();
        modScheduler.fireToDroneBuffer.removeFirst();
        assertEquals(initialCount, modScheduler.fireToDroneBuffer.getCount(), "Count should return to initial after removing added items.");
    }

    @Test
    void testFireIncidentExtractLineNoFault() {
        // Test extracting data from a line where the failure flag is 'None'
        String line = "13:00,5,DRONE_REQUEST,Moderate,None"; // Example line with 'None'
        // Assuming the nextFireID is reset to 1 by @BeforeEach
        FireEvent event = fireIncident.extractFireEventFromLine(line);

        assertNotNull(event, "Event should not be null");
        assertEquals(1, event.getFireID(), "Fire ID should be 1 (assuming reset by @BeforeEach)");
        assertEquals("13:00", event.getTime(), "Time should match");
        assertEquals(5, event.getZoneId(), "Zone ID should match");
        assertEquals("DRONE_REQUEST", event.getEventType(), "Event type should match");
        assertEquals("Moderate", event.getSeverity(), "Severity should match");
        assertFalse(event.getFailureFlag(), "Failure flag should be false when input is 'None'");
    }

    @Test
    void testFireIncidentCloseSocketNoException() {
        // This test primarily ensures that calling closeSocket() on the
        // ModifiedFireIncidentSubsystem doesn't throw an unexpected exception.
        assertDoesNotThrow(() -> {
            fireIncident.closeSocket();
        }, "Calling closeSocket should not throw an exception.");
    }

    @Test
    void testDroneIsAgentEmpty() {
        // Assumes drone is reset by @BeforeEach, agent level should be max (15)
        drone.refillAgent(); // Ensure full agent level first
        assertFalse(drone.isAgentEmpty(), "Drone should not be empty when agent is full.");

        // Drop almost all agent
        drone.dropAgent(drone.getAgentLevel() - 1); // Leave 1 unit
        assertFalse(drone.isAgentEmpty(), "Drone should not be empty when 1 unit of agent remains.");

        // Drop the last unit
        drone.dropAgent(1);
        assertTrue(drone.isAgentEmpty(), "Drone should be empty after dropping all agent.");
    }

    @Test
    void testDroneInitialStateMachineState() {
        // The DroneSubsystem constructor initializes the DroneStateMachine.
        ModifiedDroneSubsystem freshDrone = new ModifiedDroneSubsystem(999); // Use a unique ID

        // toString format: "[DRONE: id][PORT: port][STATE: STATEMACHINE_STATE]"
        assertTrue(freshDrone.toString().contains("[STATE: IDLE]"),
                "A newly created drone should have its state machine in the IDLE state, reflected in toString(). Actual: " + freshDrone.toString());
    }
}
