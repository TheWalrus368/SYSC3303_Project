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

    private LinkedList<EventStatus> drones = new LinkedList<>();  // Changed to LinkedList

    private DatagramPacket tempDatagramPacket;
    private BoundedBuffer waitingDrones;

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

            waitingDrones = new BoundedBuffer();

        } catch (SocketException e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void run() {
        RCP_Receive();
    }

    private void RCP_Receive(){
        while(true){
            try{
                // Step 1: Wait to Receive any messages
                byte[] droneRequestBuffer     = new byte[200];
                DatagramPacket requestPacket  = new DatagramPacket(droneRequestBuffer, droneRequestBuffer.length);
                receiveSocket.receive(requestPacket);

                // Step 2: Parse what they want
                String requestData            = new String(requestPacket.getData(), 0, requestPacket.getLength());
                EventStatus eventStatus = handleEvent(requestData);

                System.out.println("[STATE: " + eventStatus.getState() + "]");
                switch(eventStatus.getState()) {
                    case "READY":
                        // Step 3 (READY): Check for any unassigned fires. If there is a fire reply with fire
                        if (fireToDroneBuffer.getCount() != 0 ) {
                            String fireRequest = fireToDroneBuffer.removeFirst().toString();
                            byte[] fireRequestBuffer = fireRequest.getBytes();
                            DatagramPacket droneAcknowledgementPacket = new DatagramPacket(fireRequestBuffer,
                                    fireRequestBuffer.length,
                                    InetAddress.getLocalHost(),
                                    getDronePort(eventStatus.getEventID()));
                            System.out.println("[Scheduler -> Drone] Reply from READY request: " + fireRequest + "\n");
                            sendSocket.send(droneAcknowledgementPacket);
                            drones.pop();
                        }else{
                            System.out.println("No Fires Available");
                            tempDatagramPacket = new DatagramPacket(requestPacket.getData(), requestPacket.getLength(), requestPacket.getAddress(), RECEIVE_PORT);
                            waitingDrones.addLast(tempDatagramPacket);
                        }
                        // Step 4 (READY): Otherwise ignore the drone, make it wait
                        break;

                    case "COMPLETE":
                        // Step 3 (COMPLETE): Add to droneToFireBuffer
                        droneToFireBuffer.addLast(eventStatus.toString());
                        // Step 4 (COMPLETE): Send ACK
                        String msg = "ACK";
                        byte[] droneReply = msg.getBytes();
                        DatagramPacket droneAcknowledgementPacket = new DatagramPacket(droneReply,
                                droneReply.length,
                                InetAddress.getLocalHost(),
                                getDronePort(eventStatus.getEventID()));
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

                        if (!drones.isEmpty()) {
                            sendSocket.send((DatagramPacket) waitingDrones.removeFirst());
                        }
                        break;

                    case "WAIT":
                        // FireEvent waiting for confirmation fire is out.
                        System.out.println("[Scheduler <- FireIncidentSubsystem] " + requestData);
                        if (!drones.isEmpty()) {
                            sendSocket.send((DatagramPacket) waitingDrones.removeFirst());
                        }
                        break;
                }
            } catch(IOException e){
                e.printStackTrace();
            }
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

            EventStatus newDrone = new EventStatus(droneID, port, state, null);
            for (EventStatus existingDrone : drones) {
                if (existingDrone.getEventID() == newDrone.getEventID()) {
                    existingDrone.setState(state);
                    return existingDrone; // Drone already exists
                }
            }
            drones.add(newDrone);
            return newDrone;
        } else {
            if (data.contains("NEW FIRE")){
                return new EventStatus("FIRE");
            }else{
                return new EventStatus("WAIT");
            }
        }
    }


    public int getDronePort(int droneID){
        for (EventStatus drone: drones){
            if (drone.getEventID() == droneID){
                return drone.getPort();
            }
        }
        return -1;
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
