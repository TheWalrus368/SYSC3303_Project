import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class ModifiedFireIncidentSubsystem extends FireIncidentSubsystem{

    public DatagramSocket sendReceiveSocket;
    public final int SCHEDULER_PORT = 7000;
    public int nextFireID = 1;

    /**
     * Constructor to initialize the FireIncidentSubsystem with a CSV file path and a Scheduler.
     *
     * @param csvFilePath The path to the CSV file containing fire event data.
     */
    public ModifiedFireIncidentSubsystem(String csvFilePath) {
        super(csvFilePath);
        try{
            sendReceiveSocket = new DatagramSocket();
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    public FireEvent extractFireEventFromLine(String line){
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


    public void rpc_send(DatagramPacket dataPacket, DatagramPacket replyPacket, FireEvent fireEvent) {
        try {
            // STEP 1: Send data to Scheduler
            sendReceiveSocket.send(dataPacket);
            String data = new String(dataPacket.getData(), 0, dataPacket.getLength());
            System.out.println("[FireIncidentSubsystem -> Scheduler] Sent request: " + data);

            // STEP 2: Wait to receive ack from scheduler
            byte[] ackBuffer = new byte[200];
            DatagramPacket ackPacket = new DatagramPacket(ackBuffer, ackBuffer.length);
            sendReceiveSocket.receive(ackPacket);
            String ackData = new String(ackPacket.getData(), 0, ackPacket.getLength());
            System.out.println("[FireIncidentSubsystem <- Scheduler] Got reply: " + ackData);

            // STEP 3: Send request to scheduler for the drone reply
            String request = "REQUEST CONFIRMATION: "  + fireEvent;
            byte[] requestBuffer = request.getBytes();

            // Datagram packet to send request
            DatagramPacket requestPacket = new DatagramPacket(requestBuffer, requestBuffer.length, InetAddress.getLocalHost(), SCHEDULER_PORT);
            sendReceiveSocket.send(requestPacket);

            // STEP 4: Wait to receive the server's response passed back through the host
            sendReceiveSocket.receive(replyPacket);
            //String reply = new String(replyPacket.getData(), 0, replyPacket.getLength());
            //System.out.println("[Drone -> Scheduler -> FireIncidentSubsystem] Got reply: " + reply);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void closeSocket(){
        if(sendReceiveSocket !=null && !sendReceiveSocket.isClosed()){
            sendReceiveSocket.close();
        }
    }
}
