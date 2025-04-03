import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * The MetricsLogger class is responsible for logging events to a file and console.
 * It maintains a queue of log messages and writes them to a file periodically using
 * a daemon thread
 */
public class MetricsLogger {

    private static final List<String> logQueue = new ArrayList<>();
    private static final String LOG_FILE = "src/main/java/event-log.txt";
    private static final Object lock = new Object();

    /**
     * Starts the logging daemon that periodically flushes logs to a file.
     * This method ensures the log file is also overwritten at startup
     */
    public static void startDaemon(){

        // to overwrite the previous file entry
        try (FileWriter writer = new FileWriter(LOG_FILE, false)){
            writer.write("");
        } catch (IOException e){
            System.err.println("Error overwriting file: " + e.getMessage());
        }

        // creating a background daemon thread to flush periodically
        Thread logFlusher = new Thread(() -> {
            while (true){
                try{
                    Thread.sleep(2000);
                    flushLogs();
                } catch (InterruptedException ignored){}
            }
        });
        logFlusher.setDaemon(true);
        logFlusher.start();
    }

    /**
     * Logs an event by adding it to the queue and printing it to the console
     * @param entity The thread/shared resource creating the event
     * @param eventCode The identifier describing the event
     * @param value accepts either Long or Double type to account for time or distance values
     * @param details Additional details about the event
     */
    public static void logEvent(String entity, String eventCode, double value, String details){
        String timestamp = new SimpleDateFormat("HH:mm:ss.SSS").format(new Date());
        String event = timestamp + ", " + entity + ", " + eventCode + ", " + value + ", " + details;
        synchronized (lock) {
            logQueue.add(event);
        }
    }

    /**
     * Writes all queued logs to the log file and clears the queue.
     * This method is periodically called by the daemon thread
     */
    private static void flushLogs(){
        List<String> logsToWrite;
        synchronized (lock) {
            if (logQueue.isEmpty()) return;
            logsToWrite = new ArrayList<>(logQueue);
            logQueue.clear();
        }

        try(FileWriter writer = new FileWriter(LOG_FILE, true)){

            for (String log: logsToWrite){
                writer.write(log + "\n");
            }
            writer.flush();
        } catch (IOException e){
            System.err.println("Error writing logs: " + e.getMessage());
        }
    }
}
