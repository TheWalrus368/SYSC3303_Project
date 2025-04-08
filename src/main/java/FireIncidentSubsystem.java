import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * FireIncidentSubsystem is responsible for reading fire incident data from a CSV file
 * and sending fire events to the Scheduler.
 */
public class FireIncidentSubsystem implements Runnable {
    private final String csvFilePath;
    private final int SCHEDULER_PORT = 7000;
    private int nextFireID = 1;
    private static final int PORT = 8000;
    private final List<Thread> rpcThreads = new ArrayList<>();

    private FireEvent prevFireEvent;
    private static final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Constructor to initialize the FireIncidentSubsystem with a CSV file path and a Scheduler.
     *
     * @param csvFilePath The path to the CSV file containing fire event data.
     */
    public FireIncidentSubsystem(String csvFilePath) {
        this.csvFilePath = csvFilePath;
        prevFireEvent = null;
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

                Duration timeDifference = Duration.ZERO;
                if (prevFireEvent != null) {
                    timeDifference = calculateTimeDifference(prevFireEvent, fireEvent);
                }

                simulateTimePassing(timeDifference);

                Thread rpcThread = getThread(fireEvent);
                rpcThreads.add(rpcThread);
                rpcThread.start();
                prevFireEvent = fireEvent;
            }
        } catch (IOException ignored) { }
    }

    /**
    * Creates and configures a new Thread designed to handle the remote procedure call (RPC)
    * for reporting a specific fire event to the Scheduler.
    *
    * @param fireEvent The {@link FireEvent} object containing the details of the fire incident
    * to be reported via RPC.
    * @return A new, unstarted {@link Thread} which, when its {@code start()} method is called,
    * will execute the RPC send and receive logic for the given fire event.
    * @throws UnknownHostException If the local host IP address cannot be determined by
    * {@link InetAddress#getLocalHost()}, which is used to set
    * the destination address for the initial DatagramPacket.
    */
    private Thread getThread(FireEvent fireEvent) throws UnknownHostException {
        String newFireReport = "NEW FIRE: " + fireEvent;
        byte[] dataBuffer = newFireReport.getBytes();
        DatagramPacket dataPacket = new DatagramPacket(dataBuffer, dataBuffer.length, InetAddress.getLocalHost(), SCHEDULER_PORT);

        byte[] replyBuffer = new byte[200];
        DatagramPacket replyPacket = new DatagramPacket(replyBuffer, replyBuffer.length);

        // Create and start a new thread for each RPC send
        //new Thread(() -> rpc_send(dataPacket, replyPacket, fireEvent)).start();

        return new Thread(() -> rpc_send(dataPacket, replyPacket, fireEvent));
    }

    /**
     * Calculates the time difference between two sequential fire events based on their
     * time strings.
     *
     * @param previousEvent The preceding {@link FireEvent} in the sequence. Must not be null.
     * @param currentEvent  The current (subsequent) {@link FireEvent} in the sequence. Must not be null.
     * @return The calculated {@link Duration} representing the time elapsed between the
     * timestamps of the previous and current events.
     */
    private Duration calculateTimeDifference(FireEvent previousEvent, FireEvent currentEvent) {
        try {
            String prevTimeString = previousEvent.getTime();
            String currentTimeString = currentEvent.getTime();

            if (prevTimeString == null || currentTimeString == null) {
                System.err.println("Warning: Cannot calculate time difference, null time string detected.");
                return Duration.ZERO; // Return zero duration on null time
            }

            LocalTime previousTime = LocalTime.parse(prevTimeString, timeFormatter);
            LocalTime currentTime = LocalTime.parse(currentTimeString, timeFormatter);

            return Duration.between(previousTime, currentTime); // Return the calculated duration

        } catch (DateTimeParseException ignored) {
            System.err.printf("Warning: Could not parse time strings for difference calculation between event %d ('%s') and %d ('%s').",
                    previousEvent.getFireID(), previousEvent.getTime(),
                    currentEvent.getFireID(), currentEvent.getTime());
            return Duration.ZERO; // Return zero duration on parsing error
        } catch (Exception ignored) {
            return Duration.ZERO; // Return zero duration on other errors
        }
    }

    /**
     * Pauses the execution of the current thread for a duration derived from the
     * calculated time difference between consecutive fire events, simulating the
     * passage of time as indicated in the input file.
     * <p>
     * If the provided {@code timeDifference} is zero or negative, no sleep occurs. A warning
     * message is printed if the duration is negative. Any {@link InterruptedException} during
     * the sleep is caught and ignored, causing the method to return early.
     *
     * @param timeDifference The {@link Duration} calculated between the previous and the
     * current fire event. The actual sleep time is derived and scaled
     * from this value.
     */
    private void simulateTimePassing(Duration timeDifference) {
        if (!timeDifference.isZero() && !timeDifference.isNegative()) {
            long sleepMillis = timeDifference.toMillis();
            try {
                Thread.sleep(sleepMillis);
                // TODO remove division of 60 to simulate actual time

            } catch (InterruptedException ignored) { }
        } else if (timeDifference.isNegative()) {
            System.out.println("Warning: Current event time is before previous event time.");
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
            Print.blue("[FireIncidentSubsystem -> Scheduler] Sent request [FIRE " + fireID + "]: " + data);

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
            Print.green("[Drone -> Scheduler -> FireIncidentSubsystem] Got Drone Reply [FIRE " + fireID + "]: " + reply);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns a list of RPC threads
     * @return rpcThreads, that is the list of threads
     */
    public List<Thread> getRPCThreads(){
        return rpcThreads;
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

        // wait for all threads to end to analyze metrics
        try {
            fireIncidentThread.join();
            for (Thread rpcThread: fireIncidentSubsystem.getRPCThreads()){
                rpcThread.join();
            }

        } catch(InterruptedException e){
            e.printStackTrace();
        }

        LogAnalyzer analyzer = new LogAnalyzer();
        analyzer.analyzeMetrics();

    }

}
