public class Simulation {
    public static void main(String[] args) {
        // CSV file path
        String csvFilePath = "Iteration2/fire_events.csv";

        // Initialize Scheduler
        Scheduler scheduler = new Scheduler();

        // Initialize FireIncidentSubsystem and DroneSubsystem with references to the Scheduler
        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(csvFilePath, scheduler);
        DroneSubsystem droneSubsystem = new DroneSubsystem(scheduler);

        // Start threads
        Thread fireIncidentThread = new Thread(fireIncidentSubsystem);
        Thread schedulerThread = new Thread(scheduler);
        Thread droneThread = new Thread(droneSubsystem);

        fireIncidentThread.start();
        schedulerThread.start();
        droneThread.start();
    }
}
