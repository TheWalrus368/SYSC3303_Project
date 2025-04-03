import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * FireIncidentSubsystem is responsible for reading fire incident data from a CSV file
 * and sending fire events to the Scheduler.
 */
public class FireIncidentSubsystem implements Runnable {
    private final String csvFilePath;
    private final int SCHEDULER_PORT = 7000;
    private int nextFireID = 1;
    private static final int PORT = 8000;

    /**
     * Constructor to initialize the FireIncidentSubsystem with a CSV file path and a Scheduler.
     *
     * @param csvFilePath The path to the CSV file containing fire event data.
     */
    public FireIncidentSubsystem(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    /**
     * Reads fire incidents from a CSV file and sends them to the Scheduler.
     * Each fire event is processed sequentially with a delay between events.
     */
    @Override
    public void run() {
        try (BufferedReader reader = new BufferedReader(new FileReader(csvFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                FireEvent fireEvent = extractFireEventFromLine(line);

                String newFireReport = "NEW FIRE: " + fireEvent;
                byte[] dataBuffer = newFireReport.getBytes();
                DatagramPacket dataPacket = new DatagramPacket(dataBuffer, dataBuffer.length, InetAddress.getLocalHost(), SCHEDULER_PORT);

                byte[] replyBuffer = new byte[200];
                DatagramPacket replyPacket = new DatagramPacket(replyBuffer, replyBuffer.length);

                // Create and start a new thread for each RPC send
                new Thread(() -> rpc_send(dataPacket, replyPacket, fireEvent)).start();
            }
        } catch (IOException e) {
            System.out.println(e);
        }
    }


    /**
     * Parses a string from a line in a csv file and returns a fire event representing the fire
     * @param line String representing a line from the csv file
     * @return the fire event
     */
    private FireEvent extractFireEventFromLine(String line){
        String[] parts = line.split(",");
        String time = parts[0];
        int zoneId = Integer.parseInt(parts[1]);
        String eventType = parts[2];
        String severity = parts[3];
        String failure = parts[4];

        // Create the new fire event
        FireEvent newFire = new FireEvent(nextFireID, time, zoneId, eventType, severity, failure);
        nextFireID++;
        return newFire;
    }

    /**
     * RPC send method that sends data and waits for a synchronous reply.
     * @param dataPacket The data to send
     * @param replyPacket The reply from the server
     */
    private void rpc_send(DatagramPacket dataPacket, DatagramPacket replyPacket, FireEvent fireEvent) {
        try {
            double startResponseTime = System.currentTimeMillis();
            int fireID = fireEvent.getFireID();
            int port = PORT + fireID;
            DatagramSocket sendReceiveSocket = new DatagramSocket(port);

            // STEP 1: Send data to Scheduler
            sendReceiveSocket.send(dataPacket);
            String data = new String(dataPacket.getData(), 0, dataPacket.getLength());
            System.out.println("[FireIncidentSubsystem -> Scheduler] Sent request [FIRE " + fireID + "]: " + data);

            // STEP 2: Wait to receive ack from scheduler
            byte[] ackBuffer = new byte[200];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            sendReceiveSocket.receive(ackPacket);
            String ackData = new String(ackPacket.getData(), 0, ackPacket.getLength());
            System.out.println("[FireIncidentSubsystem <- Scheduler] Got Scheduler reply [FIRE " + fireID + "]: " + ackData);

            // STEP 3: Send request to scheduler for the drone reply
            String request = "REQUEST CONFIRMATION: [FIRE " + fireID + "]:" + fireEvent;
            byte[] requestBuffer = request.getBytes();

            // Datagram packet to send request
            double startExtinguishTime = System.currentTimeMillis(); // start time to extinguish fire
            DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length, InetAddress.getLocalHost(), SCHEDULER_PORT);
            sendReceiveSocket.send(requestPacket);

            // STEP 4: Wait to receive the server's response passed back through the host
            sendReceiveSocket.receive(replyPacket);
            String reply = new String(replyPacket.getData(), 0, replyPacket.getLength());
            double endTime = System.currentTimeMillis(); // end time of extinguished fire and response time
            double extinguishedTime = endTime - startExtinguishTime;
            double responseTime = endTime - startResponseTime;

            // record specific fire
            MetricsLogger.logEvent("FIRE " + fireID, "FIRE_EXTINGUISHED", extinguishedTime,"Time taken to extinguish fire (ms)");
            // record FireIncidentSubsystem's response time

            MetricsLogger.logEvent("FIRE_INCIDENT_SUBSYSTEM", "FIRE_RESPONSE", responseTime, "Response time of FireIncidentSubsystem (ms)");
            System.out.println("[Drone -> Scheduler -> FireIncidentSubsystem] Got Drone Reply [FIRE " + fireID + "]: " + reply);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // Start logging daemon
        MetricsLogger.startDaemon();

        // CSV file path containing fire event data
        String csvFilePath = "src/main/java/fire_events.csv";

        // Initialize FireIncidentSubsystem
        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(csvFilePath);

        // Start Thread
        Thread fireIncidentThread = new Thread(fireIncidentSubsystem, "FIRE");
        fireIncidentThread.start();

        try {
            fireIncidentThread.join(); // wait for thread to end to print metrics
        } catch(InterruptedException e){
            e.printStackTrace();
        }

        LogAnalyzer analyzer = new LogAnalyzer();
        analyzer.printMetrics();

    }

}
