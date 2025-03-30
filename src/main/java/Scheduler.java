import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.regex.*;

/**
 * The Scheduler class manages communication between the DroneSubsystem and the FireIncidentSubsystem.
 * The Scheduler assigns fire events to drones and handles incoming fire events. It uses UDP sockets to handle
 * communication
 */
class Scheduler implements Runnable{
    private static final int RECEIVE_PORT = 7000;
    private DatagramSocket receiveSocket, sendSocket;
    private BoundedBuffer fireToDroneBuffer, droneToFireBuffer;
    private final LinkedList<DroneStatus> drones = new LinkedList<>();  // Changed to LinkedList
    private Thread receiveThread;
    private String state;
    private static final String zoneFilePath = "src//main/java/sample_zone.csv";
    private static Map<Integer, Zone> zoneMap = Scheduler.loadZonesFromCSV(zoneFilePath);

    /**
     * Creates a new host by:
     * Initializing sockets to receive packets from the client and server
     * Initializing socket to send packets
     * Initialize a bounded buffer to hold unprocessed client commands
     * Initialize a bounded buffer to hold processed commands from server
     */
    public Scheduler() {
        try {
            receiveSocket           = new DatagramSocket(RECEIVE_PORT); // Receiving socket for all request
            sendSocket              = new DatagramSocket(); // For sending all packets
            fireToDroneBuffer       = new BoundedBuffer(); // Holds all unprocessed fire reports
            droneToFireBuffer       = new BoundedBuffer(); // Holds all responses from drones
            this.state              = "WAITING_TO_RECEIVE";

        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run() {
        System.out.println(this + " Ready to receive new messages");
        while(true) {
            DatagramPacket requestPacket;
            try {
                // Step 1: Wait to Receive any messages
                byte[] droneRequestBuffer = new byte[1000];
                requestPacket = new DatagramPacket(droneRequestBuffer, droneRequestBuffer.length);
                receiveSocket.receive(requestPacket);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            receiveThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    RCP_Receive(requestPacket);
                }
            });
            receiveThread.start();
        }
    }

    /**
     * Handles incoming UDP packets and processes them based on the command type.
     * This method is responsible for managing communication between the Scheduler,
     * drones, and the fire incident subsystem.
     * @param requestPacket The incoming DatagramPacket containing the request data
     */
    private void RCP_Receive(DatagramPacket requestPacket){
        try{
            int port;
            int fireID;
            // Step 2: Parse what they want
            String requestData            = new String(requestPacket.getData(), 0, requestPacket.getLength());
            EventStatus eventStatus       = handleEvent(requestData);

            switch(eventStatus.getCommand()) {
                // NEW DRONE READY TO EXTINGUISH ANY AVAILABLE FIRE
                case "IDLE":
                    this.state = "DISPATCH_DRONE";

                    // Step 3 (READY): Check for any unassigned fires. If there is a fire reply with fire
                    String fireRequest = fireToDroneBuffer.removeFirst().toString();
                    byte[] fireRequestBuffer = fireRequest.getBytes();

                    // Select an available drone to handle the fire
                    DroneStatus selectedDrone = null;
                    while (selectedDrone == null){
                        selectedDrone = getAvailableDrone();
                    }

                    // Create a packet sending the drone the fire to extinguish
                    DatagramPacket assignDroneFirePacket = new DatagramPacket(fireRequestBuffer,
                                                                    fireRequestBuffer.length,
                                                                    InetAddress.getLocalHost(),
                                                                    selectedDrone.getPort());

                    System.out.println(this + " [Scheduler -> Drone] Reply for DRONE request with: " + fireRequest);
                    sendSocket.send(assignDroneFirePacket);

                    // Update the drones status to being used
                    updateDroneState(selectedDrone.getDroneID(), "USED");
                    break;

                // DRONE INDICATING IT HAS COMPLETED EXTINGUISHING FIRE
                case "COMPLETE":
                    this.state = "NOTIFY_FIRE_EXTINGUISHED";
                    // Step 3 (COMPLETE): Add to droneToFireBuffer
                    fireID = Integer.parseInt(requestData.replaceAll(".*ID=(\\d+).*", "$1"));
                    droneToFireBuffer.addLast(fireID);

                    // Update the drone's state to READY again
                    updateDroneState(eventStatus.getDroneStatus().getDroneID(), "IDLE");

                    // Step 4 (COMPLETE): Send ACK
                    String ack = "FIRE EXTINGUISHED: FireID=" + fireID;
                    byte[] droneReply = ack.getBytes();
                    port = eventStatus.getDroneStatus().getPort();
                    DatagramPacket droneFireCompletePacket = new DatagramPacket(droneReply,
                            droneReply.length,
                            InetAddress.getLocalHost(),
                            port);
                    System.out.println(this + " [Scheduler -> Drone] reply from COMPLETE request: " + ack);
                    sendSocket.send(droneFireCompletePacket);
                    break;

                case "FIRE":
                    this.state = "NEW_FIRE";

                    // Add the fire request to the buffer
                    //fireToDroneBuffer.addLast(requestData);

                    // Step 3 (FireEvent): Add fire to buffer and Sort the fireToDroneBuffer
                    this.addSortFires(requestData);

                    // Step 4 (FireEvent): Send Ack
                    String acknowledgment = "NEW FIRE RECEIVED: " + requestData;
                    byte[] acknowledgmentBuffer = (acknowledgment).getBytes();
                    DatagramPacket acknowledgementPacket = new DatagramPacket(acknowledgmentBuffer,
                            acknowledgmentBuffer.length,
                            InetAddress.getLocalHost(),
                            requestPacket.getPort());
                    System.out.println(this + " [Scheduler -> FireIncidentSubsystem] Sent acknowledgement: " + acknowledgment + " to " + InetAddress.getLocalHost() + ":" + requestPacket.getPort());
                    sendSocket.send(acknowledgementPacket);
                    break;

                case "CONFIRMATION":
                    this.state = "CONFIRM_FIRE_EXTINGUISHED";
                    // Fire Incident Subsystem waiting for confirmation fire is out.
                    String pattern = "FireEvent\\{'ID=(\\d+)'";
                    Pattern regex = Pattern.compile(pattern);
                    Matcher matcher = regex.matcher(requestData);

                    fireID = -1;
                    if (matcher.find()) {
                        fireID = Integer.parseInt(matcher.group(1));
                    }

                    droneToFireBuffer.removeFireEventByID(fireID);

                    String confirmation = "FIRE [ID:" + fireID + "] HAS BEEN EXTINGUISHED ";
                    byte[] confirmReply = confirmation.getBytes();
                    DatagramPacket droneAcknowledgementPacket = new DatagramPacket(confirmReply,
                            confirmReply.length,
                            InetAddress.getLocalHost(),
                            requestPacket.getPort());
                    System.out.println(this + " [Scheduler -> FireIncidentSubsystem] Fire: " + fireID + " is out: " + confirmation);
                    sendSocket.send(droneAcknowledgementPacket);

                    break;
                case "FAULT":
                    System.out.println("[Scheduler <- Drone] " + requestData);
                    String unfaultedFireEvent = extractFireEvent(requestData).replace("FAULT", "NONE");
                    System.out.println("[Scheduler] Adding fire back to list " + unfaultedFireEvent);
                    // Reset the fire to no trigger a fault for the next drone
                    this.addSortFires("NEW FIRE: "+ unfaultedFireEvent);
                    break;

                case "ERROR":
                    this.state = "ERROR";
                    System.out.println(this + " SOMETHING WENT WRONG!!!");
                    break;
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

/**
 * Parses the incoming data to determine the type of event and its associated details.
 * This method identifies the event type
 * and returns an EventStatus object containing the command and relevant data.
 *
 * @param data The incoming data string to be parsed.
 * @return EventStatus An object representing the event type and associated details.
 */
    public EventStatus handleEvent(String data) {
        String pattern = "\\[DRONE: (\\d+)]\\[PORT: (\\d+)]\\[STATE: ([^]]+)]";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(data);

        // Drone event
        if (matcher.find()) {
            // Drone faulting
            if (data.contains("FAULT")){
                return new EventStatus("FAULT");
            }

            int droneID = Integer.parseInt(matcher.group(1));
            int port = Integer.parseInt(matcher.group(2));
            String state = matcher.group(3);

            // If not a fault, handle the drone event
            DroneStatus newDrone = new DroneStatus(droneID, port, state, null);
            synchronized (drones) {
                for (DroneStatus existingDrone : drones) {
                    if (existingDrone.getDroneID() == newDrone.getDroneID()) {
                        // An existing drone has COMPLETED its fire service
                        if (newDrone.getState().equals("COMPLETE")) {
                            return new EventStatus("COMPLETE", newDrone);
                        }
                    }
                }
                // New drone, register it to the list of drones
                drones.add(newDrone);
            }
            // Create and return a new event to handle a ready drone
            return new EventStatus("IDLE");
        } else {
            if (data.contains("NEW FIRE")){
                return new EventStatus("FIRE");
            }
            else if (data.contains("REQUEST CONFIRMATION:")){
                return new EventStatus("CONFIRMATION");
            }
            else{
                // Unrecognized command, should never reach this, otherwise something went wrong
                return new EventStatus("ERROR");
            }
        }
    }

    public String extractFireEvent(String data) {
        String pattern = "(FireEvent\\{'ID=\\d+', time='[^']+', zoneId=\\d+, eventType='[^']+', severity='[^']+', state='[^']+', failure='[^']+'\\})";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(data);

        if (matcher.find()) {
            return matcher.group(1); // Return only the FireEvent part
        }
        return "Could not find fire event."; // No match found
    }


    /**
     * Returns the port of the drone
     * @param droneID The drone's id
     * @return The port of the drone with the same DroneID
     */
    public int getDronePort(int droneID){
        for (DroneStatus drone: drones){
            if (drone.getDroneID() == droneID){
                return drone.getPort();
            }
        }
        return -1;
    }

    /**
     * @return The available drone from the list of drones and changes its state to "USED"
     */
    public synchronized DroneStatus getAvailableDrone(){
        for (DroneStatus drone: drones){
            if (drone.getState().equals("IDLE")){
                drone.setState("USED");
                return drone;
            }
        }
        return null;
    }

    /**
     * Changes the drone state to the next when ready
     * @param droneID The drone's id of the drone that needs to have its state updated
     * @param newState The new state that the drone needs to update to
     */
    public synchronized void updateDroneState(int droneID, String newState) {
        for (DroneStatus drone: drones){
            if (drone.getDroneID() == droneID){
                drone.setState(newState);
            }
        }
    }

    /**
     * Add the fire to the buffer and sort the buffer to optimize the selection
     * @param requestData the request for a new fire
     */
    public synchronized void addSortFires(String requestData) {
        // Add the fire request to the buffer
        fireToDroneBuffer.addLast(requestData);

        // Sort the buffer
        BoundedBuffer sortedFireBuffer  = new BoundedBuffer();
        List<String> fireEvents         = new ArrayList<>();
        List<String> severityOrder      = Arrays.asList("High", "Moderate", "Low");
        int count                       = fireToDroneBuffer.getCount();

        for (int i = 0; i < count; i++) {
            Object event = fireToDroneBuffer.removeFirst();
            if (event instanceof String) {
                fireEvents.add((String) event);
            }
        }

        // Sorting algorithm for the fire events
        Collections.sort(fireEvents, new Comparator<String>() {
            public int compare(String e1, String e2) {
                Pattern pattern     = Pattern.compile("severity='(High|Moderate|Low)'");
                Matcher matcher1    = pattern.matcher(e1);
                Matcher matcher2    = pattern.matcher(e2);
                int rank1 = severityOrder.size();
                int rank2 = severityOrder.size();
                if (matcher1.find()) {
                    rank1 = severityOrder.indexOf(matcher1.group(1));
                }
                if (matcher2.find()) {
                    rank2 = severityOrder.indexOf(matcher2.group(1));
                }
                return rank1 - rank2;
            }
        });

        //System.out.println("SORTED FIRE EVENT LIST: " + fireEvents);
        for (String event : fireEvents) {
            sortedFireBuffer.addLast(event);
        }

        // Set the buffer to the sorted buffer
        this.fireToDroneBuffer = sortedFireBuffer;
    }

    /**
     * Loads zone data from a CSV file and populates the zoneMap with Zone objects.
     * The method parses the file, extracts zone coordinates, and creates Zone objects
     * which are then stored in the zoneMap for later retrieval.
     *
     * @param filePath The path to the CSV file containing zone data.
     */
    public static Map<Integer, Zone> loadZonesFromCSV(String filePath) {
         Map<Integer, Zone> zoneMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    int zoneId = Integer.parseInt(parts[0]);
                    String[] startCoords = parts[1].replace("(", "").replace(")", "").split(";");
                    String[] endCoords = parts[2].replace("(", "").replace(")", "").split(";");

                    double startX = Double.parseDouble(startCoords[0]);
                    double startY = Double.parseDouble(startCoords[1]);
                    double endX = Double.parseDouble(endCoords[0]);
                    double endY = Double.parseDouble(endCoords[1]);

                    zoneMap.put(zoneId, new Zone(zoneId, startX, startY, endX, endY));
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading CSV file: " + e.getMessage());
        }
        return zoneMap;
    }

    @Override
    public String toString(){
        return "[SCHEDULER][STATE: " + this.state + "]";
    }


    /**
     * @param zoneId The ID of the zone to retrieve.
     * @return Zone The Zone object corresponding to the given ID, or null if the zone ID is not found.
     */
    public static Zone getZone(int zoneId) {
        return zoneMap.get(zoneId);
    }

    public static void main(String[] args) {
        // Initialize the Scheduler, responsible for managing communication between subsystems
        Scheduler scheduler = new Scheduler();

        // Start Thread
        Thread schedulerThread = new Thread(scheduler, "SCHEDULER");
        schedulerThread.start();
    }
}
