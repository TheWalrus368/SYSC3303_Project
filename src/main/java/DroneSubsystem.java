import java.io.IOException;
import java.net.*;
import java.util.regex.*;

/**
 * The subsystem for a single drone, that sends requests from the scheduler for fire incidents
 * and performs various tasks in accordance to its state.
 */
public class DroneSubsystem implements Runnable {
    private int droneID;
    //private final DroneStateMachine stateMachine;
    private int agentLevel;
    private FireEvent currentFireEvent;
    public FireEvent lastFireEvent;
    private static final int MAX_AGENT_CAP = 15; // Max payload is 15kg
    private static final int SPEED = 300;
    private DatagramSocket sendSocket, receiveSocket;
    private static final int BASE_PORT = 6000;
    private static final int SCHEDULER_PORT = 7000;
    private int DRONE_PORT;
    private double droneX;
    private double droneY;
    private Zone currentZone;
    private Zone nextDestination;
    public static final Zone BASE_ZONE = new Zone(0, 0, 0, 0, 0);
    private final DroneStateMachine stateMachine = new DroneStateMachine(this);

    /**
     * Constructs a DroneSubsystem with a reference to the Scheduler.
     * Initializes the drone state machine and sets the agent level to full capacity.
     *
     * @param droneID the ID of the drone
     */
    public DroneSubsystem(int droneID) {
        this.droneID = droneID;
        //this.stateMachine = new DroneStateMachine(this);
        this.agentLevel = MAX_AGENT_CAP; // Start with full agent
        this.DRONE_PORT = BASE_PORT + droneID;
        this.currentFireEvent = null;
        this.lastFireEvent = null;
        this.nextDestination = null;
        this.currentZone = BASE_ZONE;

        try {
            sendSocket      = new DatagramSocket();
            receiveSocket   = new DatagramSocket(DRONE_PORT);

        } catch (SocketException ignored) {
            System.exit(1);
        }
    }

    /**
     * The main execution loop for the drone.
     * It continuously processes state transitions using the state machine.
     */
    @Override
    public void run() {
        // handle events for state machine
        while(true) {
            try {
                stateMachine.handleState();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Sends a request to the scheduler to be assigned a fire incident and receives it.
     *
     * @param requestPacket the packet that is sent to the scheduler to request for a fire event
     * @param receivePacket the packet that is received from the scheduler
     * @return the received packet from the scheduler after the request
     */
    public synchronized DatagramPacket rpc_send(DatagramPacket requestPacket, DatagramPacket receivePacket){
       try{
           // STEP 1: Send a request to scheduler for any new fires / register to drone to the schedulers knowledge
           String sendData            = new String(requestPacket.getData(), 0, requestPacket.getLength());
           System.out.println("[Drone -> Scheduler]" + this + " Sending Drone request: " + sendData);
           sendSocket.send(requestPacket);

           // STEP 2: Wait to receive reply from host with new data
           receiveSocket.receive(receivePacket);
           String receiveData            = new String(receivePacket.getData(), 0, receivePacket.getLength());
           System.out.println("[Drone <- Scheduler]" + this + " Drone received: " + receiveData);
       }
       catch (IOException e){
           e.printStackTrace();
       }
        return receivePacket;
    }

    /**
     * Fetches a fire event task from the scheduler.
     * If the last fire event is not yet fully extinguished, the drone will continue with it.
     *
     * @return The fire event task assigned to the drone.
     */
    public FireEvent fetchFireTask() {
        System.out.println(this + " Ready to service any new fires");

        if (currentFireEvent == null){
            // Initial Request Packet
            String requestData              = this + " READY: Ready to service any new fires";
            byte[] requestBuffer            = requestData.getBytes();
            DatagramPacket requestPacket    = null;
            try {
                requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length,
                        InetAddress.getLocalHost(), SCHEDULER_PORT);
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }

            // Response packet with any new fire from the Scheduler
            byte[] receiveBuffer           = new byte[1000];
            DatagramPacket receivePacket   = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            // Send packet and BLOCK on reception for data (new fire event)
            DatagramPacket dataPacket = rpc_send(requestPacket, receivePacket);

            // Handle the client request and send ack back to Scheduler
            String data = new String(dataPacket.getData(), 0, dataPacket.getLength());
            System.out.println(this + " Received: " + data + " from Scheduler(" + dataPacket.getAddress() + ":" + dataPacket.getPort() + ")");

            // Process the request returned from the scheduler and return the fire event
            currentFireEvent        = parseDataToFireEvent(data);
            this.nextDestination    = Scheduler.getZone(currentFireEvent.getZoneId());

            }
        return currentFireEvent;
    }

    /**
     * Parses the fire event from a string and extracts the information to create a fire event.
     * @param data The fire event data from the packet as a string
     * @return the fire event that was created from the extracted data
     */
    public FireEvent parseDataToFireEvent(String data) {
        // Updated regex pattern to correctly extract values, including failure
        String pattern = "NEW FIRE: FireEvent\\{'ID=(\\d+)', time='([^']+)', zoneId=(\\d+), eventType='([^']+)', severity='([^']+)', state='[^']+', failure='([^']+)'\\}";
        Pattern regex = Pattern.compile(pattern);
        Matcher matcher = regex.matcher(data);

        if (matcher.find()) {
            int fireID = Integer.parseInt(matcher.group(1));
            String time = matcher.group(2);
            int zoneId = Integer.parseInt(matcher.group(3));
            String eventType = matcher.group(4);
            String severity = matcher.group(5);
            String failure = matcher.group(6);
            return new FireEvent(fireID, time, zoneId, eventType, severity, failure);
        }
        return null; // No match found
    }


    /**
     * Sends an acknowledgment to the Scheduler and waits for a response.
     * If a response is received, it prints the data from the Scheduler.
     *
     * @param ack The acknowledgment message to be sent.
     */
    public void sendAck(String ack){
        // Initial Request Packet
        byte[] requestBuffer            = ack.getBytes();
        try {
            DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length,
                                                              InetAddress.getLocalHost(), SCHEDULER_PORT);

            // Response packet with any new fire from the Scheduler
            byte[] receiveBuffer           = new byte[200];
            DatagramPacket receivePacket   = new DatagramPacket(receiveBuffer, receiveBuffer.length);

            // Send packet and BLOCK on reception for data (new fire event)
            DatagramPacket dataPacket = rpc_send(requestPacket, receivePacket);

            // Handle the client request and send ack back to Scheduler
            String data = new String(dataPacket.getData(), 0, dataPacket.getLength());
            System.out.println(this + " Received: " + data + " from Scheduler(" + dataPacket.getAddress() + ":" + dataPacket.getPort() + ")");
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Updates the state to completed and sends an acknowledgment to the Scheduler
     * indicating that the fire has been extinguished, along with details of the last fire event.
     */
    public void returnFireCompleted(){
        String ack = this + " COMPLETED: Fire has been extinguished " + lastFireEvent;
        sendAck(ack);
    }

    /**
     *  Sends a failure message back to the scheduler
     */
    public void returnFailure(){
        String fail = this + " FAULT: This Fire has failed with " + currentFireEvent;
        sendAck(fail);
    }

    /**
     * Refills the agent to its maximum capacity
     * and simulates the drone's travel back to the base zone.
     */
    public void refillAgent() {
        System.out.println(this + " Refilling agent... ");
        this.agentLevel = MAX_AGENT_CAP;
    }

    /**
     * Simulates the drone travelling towards the zone of the fire event
     * @param zone The zone of the fire event
     */
    public void simulateDroneTravel(Zone zone) {
        if (this.currentZone.equals(zone)){
            System.out.println(this + " Already at ZONE " + zone.getID());
            return;
        }
        // Calculate the center of the target zone
        double centerX = (zone.getStartX() + zone.getEndX()) / 2.0;
        double centerY = (zone.getStartY() + zone.getEndY()) / 2.0;

        // Calculate the distance to the center
        double distance = Math.sqrt(Math.pow(centerX - droneX, 2) + Math.pow(centerY - droneY, 2));

        // Assume a fixed speed (units per second)

        // Calculate travel time in milliseconds
        long travelTimeMillis = (long) ((distance / SPEED) * 1000);

        try {
            System.out.println(this + " Traveling to: ZONE " + zone.getID() + " (" + centerX + ", " + centerY + "), ETA: " + travelTimeMillis/1000 + "s");

            // Simulate time travelling by sleeping
            Thread.sleep(travelTimeMillis);

            // Update the drones position once its reached its destination
            this.droneX = centerX;
            this.droneY = centerY;

            System.out.println(this + " arrived at destination.");
            this.currentZone = zone;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(this + " Travel interrupted!");
        }
    }

    /**
     * Checks if the agent tank is empty.
     *
     * @return true if the agent tank is empty, false otherwise.
     */
    public boolean isAgentEmpty() {
        return agentLevel == 0;
    }


    public Zone getNextDestination(){
        return this.nextDestination;
    }


    public Zone getCurrentZone(){
        return this.currentZone;
    }

    public int getAgentLevel(){
        return this.agentLevel;
    }

    public FireEvent getCurrentFireEvent(){
        return this.currentFireEvent;
    }

    public void dropAgent(int amount){
        this.agentLevel = this.agentLevel - amount;
    }

    public boolean currentFireExtinguished(){
        if(currentFireEvent.getRemainingWaterNeeded() <= 0){
            lastFireEvent       = currentFireEvent;
            currentFireEvent    = null;
            return true;
        };
        return false;
    }

    @Override
    public String toString() {
        return "[DRONE: " + this.droneID + "]" + "[PORT: " + this.DRONE_PORT + "]" + "[STATE: " + this.stateMachine.getState().toUpperCase() + "]";
    }

    public static void main(String[] args) {
        // Initialize DroneSubsystems
        DroneSubsystem droneSubsystem1 = new DroneSubsystem(100);
        DroneSubsystem droneSubsystem2 = new DroneSubsystem(200);
        DroneSubsystem droneSubsystem3 = new DroneSubsystem(300);
        DroneSubsystem droneSubsystem4 = new DroneSubsystem(400);

        // Start threads
        Thread droneThread1 = new Thread(droneSubsystem1, "DRONE 1");
        Thread droneThread2 = new Thread(droneSubsystem2, "DRONE 2");
        Thread droneThread3 = new Thread(droneSubsystem3, "DRONE 3");
        Thread droneThread4 = new Thread(droneSubsystem4, "DRONE 4");

        // Begin execution of subsystems
        droneThread1.start();
        droneThread2.start();
        droneThread3.start();
        droneThread4.start();
    }

}