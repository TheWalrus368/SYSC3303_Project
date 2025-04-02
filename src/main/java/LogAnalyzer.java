import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LogAnalyzer {
    private static final String LOG_FILE = "src/main/java/event-log.txt";
    private List<Long> responseTimes = new ArrayList<>();
    private List<Long> extinguishTimes = new ArrayList<>();
    private List<Double> distanceTravelled = new ArrayList<>();

    /**
     * Reads the event log file and returns its contents as a list of log entries
     *
     * @return list of string log entries
     */
    private List<String> readLogsFromFile() {
        List<String> logs = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(LOG_FILE))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logs.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public void logResponseTime(long time){
        responseTimes.add(time);
    }

    public void logExtinguishTime(long time){
        extinguishTimes.add(time);
    }

    public void logDistance(double distance){
        distanceTravelled.add(distance);
        System.out.println("\n\nTOTAL DISTANCE TRAVELLED: " + distanceTravelled);
    }

    public void printMetrics(){
        List<String> logs = readLogsFromFile();
        System.out.println("----PERFORMANCE METRICS ------");
        System.out.println("LOGS: " + logs);
//        System.out.println("Average response time: ");
//        System.out.println("Total fire extinguish time: ");
//        System.out.println("Total distance traveled: " + distanceTravelled);
    }
}
