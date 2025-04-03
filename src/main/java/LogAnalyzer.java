import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LogAnalyzer {
    private static final String LOG_FILE = "src/main/java/event-log.txt";
    private static final String METRICS_FILE = "src/main/java/metrics-log.txt";
    private ArrayList<Double> schedulerTimes = new ArrayList<>();
    private ArrayList<Double> fireTimes = new ArrayList<>();
    private Map<String, List<Double>> droneTimes = new HashMap<>();
    private Map<String, Double> extinguishedTimes = new HashMap<>();
    private Map<String, Double> distanceTravelled = new HashMap<>();

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

    /**
     * Analyzes the event log file and calculates for average response times, busy times,
     * waiting times, throughput, and utilization
     */
    public void analyzeMetrics() {
        List<String> logs = readLogsFromFile();
        // clear previous data
        schedulerTimes.clear();
        fireTimes.clear();
        extinguishedTimes.clear();
        distanceTravelled.clear();
        droneTimes.clear();

        for (String log: logs){
            String[] parts = log.split(",");

            double timestamp = Double.parseDouble(parts[0].trim().split(":")[2]);
            String entity = parts[1].trim();
            String eventCode = parts[2].trim();
            double value = Double.parseDouble(parts[3].trim());

            // collect distance required to reach target fire zone
            if (eventCode.equals("DRONE_TRAVELS")){
                distanceTravelled.put(entity, value);

            // collect response times of Scheduler
            } else if (eventCode.equals("SCHEDULER_RESPONSE")){
                schedulerTimes.add(value);

                // collect response times of FireIncidentSubsystem
            } else if (eventCode.equals("FIRE_RESPONSE")){
                fireTimes.add(value);

                // collect response times of each drone
            } else if (eventCode.equals("DRONE_RESPONSE")){
                // if drone entity not captured yet, create map and its array list
                droneTimes.putIfAbsent(entity, new ArrayList<>());

                // append if drone entity already exists in map
                droneTimes.get(entity).add(value);

            // collect extinguished fire times
            } else if (eventCode.equals("FIRE_EXTINGUISHED")) {
                extinguishedTimes.put(entity, value);
            }
        }
        calculateMetrics();
    }

    private void calculateMetrics(){
        double totalExtinguishedTime = 0.0;
        Map<String, Double> avgDroneTimes = new HashMap<>();

        // calculate for total fire extinguishing time
        for (double extinguishedTime: extinguishedTimes.values()){
            totalExtinguishedTime += extinguishedTime;
        }

        // calculate average response time for Scheduler
        double avgSchedulerTime = schedulerTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // calculate average response time for FireIncidentSubsystem
        double avgFireTime = fireTimes.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        // calculate average response time of each drone
        for (Map.Entry<String, List<Double>> entry: droneTimes.entrySet()){
            String droneId = entry.getKey();
            List<Double> times = entry.getValue();

            // calculating drone's average
            double avgTime = times.stream().mapToDouble(Double::doubleValue).average().orElse(0);

            avgDroneTimes.put(droneId, avgTime);
        }

        // calculate overall average drone response time
        double overallDroneResponseTime = avgDroneTimes.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        writeMetricsLog(totalExtinguishedTime, avgDroneTimes, overallDroneResponseTime, avgSchedulerTime, avgFireTime);

    }

    private void writeMetricsLog(double totalExtinguishedTime, Map<String, Double> avgDroneTimes, double overallDroneResponseTime, double avgSchedulerTime, double avgFireTime){
        // to overwrite the previous file entry
        try (FileWriter writer = new FileWriter(METRICS_FILE, false)){
            writer.write("");
        } catch (IOException e){
            System.err.println("Error overwriting file: " + e.getMessage());
        }

        // write calculated metrics into log
        try(FileWriter writer = new FileWriter(METRICS_FILE, true)){

            writer.write("------PERFORMANCE METRICS ------\n");

            writer.write("Total Extinguishing Time: " + totalExtinguishedTime + " ms\n");

            // write each extinguished time
            writer.write("Extinguished Times for each Fire Incident:\n");
            for (Map.Entry<String, Double> entry : extinguishedTimes.entrySet()) {
                String entity = entry.getKey();
                double extinguishedTime = entry.getValue();
                writer.write(entity + ": " + extinguishedTime + " ms\n");
            }

            // write each drone's response time
            writer.write("Drone Response Times:\n");
            for (Map.Entry<String, Double> entry : avgDroneTimes.entrySet()) {
                String droneId = entry.getKey();
                double avgTime = entry.getValue();
                writer.write("Drone " + droneId + ": " + avgTime + " ms\n");
            }

            writer.write("\nOverall Average Drone Response Time: " + overallDroneResponseTime + " ms\n");
            writer.write("Average Scheduler Response Time: " + avgSchedulerTime + " ms\n");
            writer.write("Average Fire Incident Response Time: " + avgFireTime + " ms\n");

            writer.flush();
        } catch (IOException e){
            System.err.println("Error writing logs: " + e.getMessage());
        }

    }


    /**
     * Prints the calculated metrics
     */
    public void printMetrics(){
        List<String> logs = readLogsFromFile();
        System.out.println("----PERFORMANCE METRICS ------");
        System.out.println("LOGS: " + logs); // for testing purposes
    }
}
