public class ModifiedFireIncidentSubsystem extends FireIncidentSubsystem{
    /**
     * Constructor to initialize the FireIncidentSubsystem with a CSV file path and a Scheduler.
     *
     * @param csvFilePath The path to the CSV file containing fire event data.
     */
    public ModifiedFireIncidentSubsystem(String csvFilePath) {
        super(csvFilePath);
    }
}
