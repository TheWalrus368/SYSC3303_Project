/**
 * The Simulation class initializes and starts the fire incident simulation.
 * It creates instances of the FireIncidentSubsystem, Scheduler, and DroneSubsystem,
 * and starts them as separate threads to handle fire events and drone operations concurrently.
 */
public class Simulation {
    public static void main(String[] args) {
        // CSV file path containing fire event data
        String csvFilePath = "Iteration2/fire_events.csv";

       // Initialize the Scheduler, responsible for managing communication between subsystems
        Scheduler scheduler = new Scheduler();

        // Initialize FireIncidentSubsystem and DroneSubsystem with references to the Scheduler
        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(csvFilePath, scheduler);
        DroneSubsystem droneSubsystem = new DroneSubsystem(scheduler);

        // Start threads
        Thread fireIncidentThread = new Thread(fireIncidentSubsystem);
        Thread schedulerThread = new Thread(scheduler);
        Thread droneThread = new Thread(droneSubsystem);

        // Begin execution of subsystems
        fireIncidentThread.start();
        schedulerThread.start();
        droneThread.start();
    }
}
