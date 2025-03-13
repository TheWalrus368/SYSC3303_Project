import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * FireIncidentSubsystem is responsible for reading fire incident data from a CSV file
 * and sending fire events to the Scheduler.
 */
public class FireIncidentSubsystem implements Runnable {
    private final String csvFilePath;
    private final Scheduler scheduler;

     /**
     * Constructor to initialize the FireIncidentSubsystem with a CSV file path and a Scheduler.
     * 
     * @param csvFilePath The path to the CSV file containing fire event data.
     * @param scheduler   The scheduler that manages fire events.
     */
    public FireIncidentSubsystem(String csvFilePath, Scheduler scheduler) {
        this.csvFilePath = csvFilePath;
        this.scheduler = scheduler;
    }

    /**
     * Reads fire incidents from a CSV file and sends them to the Scheduler.
     * Each fire event is processed sequentially with a delay between events.
     */
    @Override
    public void run() {
        // Parse the csv file and create fire events
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Parse the CSV line into a FireEvent object
                String[] parts = line.split(",");
                String time = parts[0];
                int zoneId = Integer.parseInt(parts[1]);
                String eventType = parts[2];
                String severity = parts[3];

                FireEvent fireEvent = new FireEvent(time, zoneId, eventType, severity);

                System.out.println("[FireIncidentSubsystem] NEW FIRE: Sending event to Scheduler: " + fireEvent);
                scheduler.receiveFireEvent(fireEvent); // Send the event to Scheduler

                FireEvent receivedEvent = scheduler.receiveDroneAcknowledgement(); // Wait to receive a response
                System.out.println("[FireIncidentSubsystem] Received response from Scheduler " + receivedEvent);

                Thread.sleep(1000); // Simulate delay between events
                System.out.print("\n");
            }
        } catch (IOException | InterruptedException e) {
            System.out.println(e);
        }
    }

}
