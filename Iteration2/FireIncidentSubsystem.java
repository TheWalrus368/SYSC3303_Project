import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FireIncidentSubsystem implements Runnable {
    private final String csvFilePath;
    private final Scheduler scheduler;

    public FireIncidentSubsystem(String csvFilePath, Scheduler scheduler) {
        this.csvFilePath = csvFilePath;
        this.scheduler = scheduler;
    }

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
