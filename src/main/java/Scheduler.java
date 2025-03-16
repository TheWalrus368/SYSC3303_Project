import java.io.*;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.*;
import java.util.regex.*;

class Scheduler implements Runnable{
    private static final Map<Integer, Zone> zoneMap = new HashMap<>();
    private static final int RECEIVE_PORT = 7000;
    private DatagramSocket receiveSocket, sendSocket;
    private BoundedBuffer fireToDroneBuffer, droneToFireBuffer;
    private LinkedList<DroneStatus> drones = new LinkedList<>();  // Changed to LinkedList
    private Thread receiveThread;

    /**
     * Creates a new host by:
     * Initializing sockets to receive packets from the client and server
     * Initializing socket to send packets
     * Initialize a bounded buffer to hold unprocessed client commands
     * Initialize a bounded buffer to hold processed commands from server
     */
    public Scheduler(String zoneCSV) {
        try {
            receiveSocket           = new DatagramSocket(RECEIVE_PORT); // Receiving socket for all request
            sendSocket              = new DatagramSocket(); // For sending all packets
            fireToDroneBuffer       = new BoundedBuffer(); // Holds all unprocessed fire reports
            droneToFireBuffer       = new BoundedBuffer(); // Holds all responses from drones
            loadZonesFromCSV(zoneCSV);

        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run() {
        while(true) {
            DatagramPacket requestPacket;
            try {
                // Step 1: Wait to Receive any messages
                byte[] droneRequestBuffer = new byte[200];
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

    private void RCP_Receive(DatagramPacket requestPacket){
        try{
            int port;
            int fireID;
            // Step 2: Parse what they want
            String requestData            = new String(requestPacket.getData(), 0, requestPacket.getLength());
            EventStatus eventStatus       = handleEvent(requestData);

            switch(eventStatus.getCommand()) {
                // NEW DRONE READY TO EXTINGUISH ANY AVAILABLE FIRE
                case "READY":
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

                    System.out.println("[Scheduler -> Drone] Reply for DRONE request with:" + fireRequest);
                    sendSocket.send(assignDroneFirePacket);

                    // Update the drones status to being used
                    updateDroneState(selectedDrone.getDroneID(), "USED");
                    break;

                // DRONE INDICATING IT HAS COMPLETED EXTINGUISHING FIRE
                case "COMPLETE":
                    // Step 3 (COMPLETE): Add to droneToFireBuffer
                    fireID = Integer.parseInt(requestData.replaceAll(".*ID=(\\d+).*", "$1"));
                    droneToFireBuffer.addLast(fireID);

                    // Update the drone's state to READY again
                    updateDroneState(eventStatus.getDroneStatus().getDroneID(), "READY");

                    // Step 4 (COMPLETE): Send ACK
                    String ack = "FIRE EXTINGUISHED: FireID=" + fireID;
                    byte[] droneReply = ack.getBytes();
                    port = eventStatus.getDroneStatus().getPort();
                    DatagramPacket droneFireCompletePacket = new DatagramPacket(droneReply,
                            droneReply.length,
                            InetAddress.getLocalHost(),
                            port);
                    System.out.println("[Scheduler -> Drone] reply from COMPLETE request: " + ack);
                    sendSocket.send(droneFireCompletePacket);
                    break;

                case "FIRE":
                    // Step 3 (FireEvent): Add fire to buffer
                    fireToDroneBuffer.addLast(requestData);

                    // Step 4 (FireEvent): Send Ack
                    String acknowledgment = "NEW FIRE RECEIVED: " + requestData;
                    byte[] acknowledgmentBuffer = (acknowledgment).getBytes();
                    DatagramPacket acknowledgementPacket = new DatagramPacket(acknowledgmentBuffer,
                            acknowledgmentBuffer.length,
                            InetAddress.getLocalHost(),
                            requestPacket.getPort());
                    System.out.println("[Scheduler -> FireIncidentSubsystem] Sent acknowledgement: " + acknowledgment + " to " + InetAddress.getLocalHost() + ":" + requestPacket.getPort());
                    sendSocket.send(acknowledgementPacket);
                    break;

                case "CONFIRMATION":
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
                    System.out.println("[Scheduler -> FireIncidentSubsystem] Fire: " + fireID + " is out: " + confirmation);
                    sendSocket.send(droneAcknowledgementPacket);

                    break;

                case "ERROR":
                    System.out.println("SOMETHING WENT WRONG!!!");
                    break;
            }
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    public EventStatus handleEvent(String data) {
        //System.out.println("\n" + "data: " + data + "\n");
        String pattern = "\\[DRONE: (\\d+)\\]\\[PORT: (\\d+)\\]\\[STATE: ([^\\]]+)\\]";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(data);

        if (matcher.find()) {
            int droneID = Integer.parseInt(matcher.group(1));
            int port = Integer.parseInt(matcher.group(2));
            String state = matcher.group(3);

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
            return new EventStatus("READY");
        } else {
            if (data.contains("NEW FIRE")){
                return new EventStatus("FIRE");
            }else if (data.contains("REQUEST CONFIRMATION:")){
                return new EventStatus("CONFIRMATION");
            }
            else{
                // Unrecognized command, should never reach this, otherwise something went wrong
                return new EventStatus("ERROR");
            }
        }
    }

    public int getDronePort(int droneID){
        for (DroneStatus drone: drones){
            if (drone.getDroneID() == droneID){
                return drone.getPort();
            }
        }
        return -1;
    }

    public synchronized DroneStatus getAvailableDrone(){
        for (DroneStatus drone: drones){
            if (drone.getState().equals("READY")){
                drone.setState("USED");
                return drone;
            }
        }
        return null;
    }

    public synchronized void updateDroneState(int droneID, String newState) {
        for (DroneStatus drone: drones){
            if (drone.getDroneID() == droneID){
                drone.setState(newState);
            }
        }
    }

    public void loadZonesFromCSV(String filePath) {
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
    }

    public static Zone getZone(int zoneId) {
        return zoneMap.get(zoneId);
    }
}
