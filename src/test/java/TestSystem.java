import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

public class TestSystem {

    private ModifiedDroneSubsystem drone;
    private ModifiedScheduler scheduler;
    private ModifiedFireIncidentSubsystem fireIncident;

    @BeforeAll
    static void setup() throws Exception {


        // Init Fire Subsystem
        String csvFilePath = "src/main/java/fire_events.csv";
        FireIncidentSubsystem fireIncidentSubsystem = new FireIncidentSubsystem(csvFilePath);
        Thread fireIncidentThread = new Thread(fireIncidentSubsystem, "FIRE");
    }
}
