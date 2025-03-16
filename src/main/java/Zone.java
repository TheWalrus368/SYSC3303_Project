/**
 * Represents a zone with a unique ID and coordinates defining its boundaries.
 * The zone is defined by a starting point (startX, startY) and an ending point (endX, endY).
 */
class Zone {
    private final int ID;
    private final double startX, startY, endX, endY;

    /**
     * Constructs a new Zone with the specified ID and boundary coordinates.
     *
     * @param name   The unique ID of the zone.
     * @param startX The starting X-coordinate of the zone.
     * @param startY The starting Y-coordinate of the zone.
     * @param endX   The ending X-coordinate of the zone.
     * @param endY   The ending Y-coordinate of the zone.
     */
    public Zone(int name, double startX, double startY, double endX, double endY) {
        this.ID = name;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    /**
     * @return int The ID of the zone.
     */
    public int getID() { return ID; }

    /**
     * @return double The starting X-coordinate.
     */
    public double getStartX() { return startX; }

    /**
     * @return double The starting Y-coordinate.
     */
    public double getStartY() { return startY; }

    /**
     * @return double The ending X-coordinate.
     */
    public double getEndX() { return endX; }

    /**
     * @return double The ending Y-coordinate.
     */
    public double getEndY() { return endY; }
}