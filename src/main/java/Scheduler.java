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
    // Array for confirmations

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
            // Check if any confirmation fires are done()
            RCP_Receive();
        }
    }

    private void RCP_Receive(){
        try{
            // Step 1: Wait to Receive any messages
            byte[] droneRequestBuffer     = new byte[200];
            DatagramPacket requestPacket  = new DatagramPacket(droneRequestBuffer, droneRequestBuffer.length);
            receiveSocket.receive(requestPacket);

            // Step 2: Parse what they want
            String requestData            = new String(requestPacket.getData(), 0, requestPacket.getLength());
            EventStatus eventStatus       = handleEvent(requestData);

            //System.out.println("[STATE: " + eventStatus.getState() + "]");
            switch(eventStatus.getCommand()) {
                case "READY":
                    // NEW DRONE READY TO EXTINGUISH ANY AVAILABLE FIRE
                    // Step 3 (READY): Check for any unassigned fires. If there is a fire reply with fire
                    if (fireToDroneBuffer.getCount() != 0 ) {
                        String fireRequest = fireToDroneBuffer.removeFirst().toString();
                        byte[] fireRequestBuffer = fireRequest.getBytes();

                        // Select an available drone to handle the fire
                        DroneStatus selectedDrone = null;
                        while (selectedDrone == null){
                            selectedDrone = getAvailableDrone();
                        }

                        DatagramPacket droneAcknowledgementPacket = new DatagramPacket(fireRequestBuffer,
                                fireRequestBuffer.length,
                                InetAddress.getLocalHost(),
                                getDronePort(selectedDrone.getPort()));
                        System.out.println("[Scheduler -> Drone] Reply from READY request: " + fireRequest + "\n");
                        sendSocket.send(droneAcknowledgementPacket);
                        // Update the drones status to being used
                        updateDroneState(selectedDrone.getDroneID(), "USED");
                    }
                    else {
                        System.out.println("No Fires Available");
                        // Step 4 (READY): If there are no fires to service, otherwise ignore the drone, make it wait
                        break;
                    }

                case "COMPLETE":
                    // Step 3 (COMPLETE): Add to droneToFireBuffer
                    droneToFireBuffer.addLast(eventStatus.getDroneStatus().toString());

                    // Update the drone's state to READY again
                    updateDroneState(eventStatus.getDroneStatus().getDroneID(), "READY");

                    // Step 4 (COMPLETE): Send ACK
                    String msg = "ACK";
                    byte[] droneReply = msg.getBytes();
                    DatagramPacket droneAcknowledgementPacket = new DatagramPacket(droneReply,
                            droneReply.length,
                            InetAddress.getLocalHost(),
                            getDronePort(eventStatus.getDroneStatus().getPort()));
                    System.out.println("[Scheduler -> Drone] reply from COMPLETE request: " + msg + "\n");
                    sendSocket.send(droneAcknowledgementPacket);

                    // Step 5 (COMPLETE): Send to FireIncident
                    byte[] confirmReply = requestData.getBytes();
                    DatagramPacket confirmPacket = new DatagramPacket(confirmReply,
                            confirmReply.length,
                            InetAddress.getLocalHost(),
                            FireIncidentSubsystem.getPort());
                    System.out.println("[Scheduler -> FireIncidentSubsystem] Fire Extinguished: " + msg + "\n");
                    sendSocket.send(confirmPacket);
                    break;

                case "FIRE":
                    // Step 3 (FireEvent): Add fire to buffer
                    fireToDroneBuffer.addLast(requestData);

                    // Step 4 (FireEvent): Send Ack
                    String acknowledgment = "ACK(" + requestData + ")";
                    byte[] acknowledgmentBuffer = (acknowledgment).getBytes();
                    DatagramPacket acknowledgementPacket = new DatagramPacket(acknowledgmentBuffer,
                            acknowledgmentBuffer.length,
                            InetAddress.getLocalHost(),
                            requestPacket.getPort());
                    System.out.println("[Scheduler -> FireIncidentSubsystem] Sent immediate " + acknowledgment + " to " + InetAddress.getLocalHost() + ":" + requestPacket.getPort());
                    sendSocket.send(acknowledgementPacket);
                    break;

                case "CONFIRMATION":
                    // Fire Incident Subsystem waiting for confirmation fire is out.
                    // Look for the fire and confirm it's been extinguished
                    String response = droneToFireBuffer.
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
            for (DroneStatus existingDrone : drones) {
                if (existingDrone.getDroneID() == newDrone.getDroneID()) {
                    // An existing drone has COMPLETED its fire service
                    if (newDrone.getState().equals("COMPLETED")){
                        return new EventStatus("COMPLETED", newDrone);
                    }
                }
            }
            // New drone, register it to the list of drones
            drones.add(newDrone);

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

    public DroneStatus getAvailableDrone(){
        for (DroneStatus drone: drones){
            if (drone.getState().equals("READY")){
                return drone;
            }
        }
        return null;
    }

    public void updateDroneState(int droneID, String newState) {
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
