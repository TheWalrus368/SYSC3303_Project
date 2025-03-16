class Zone {
    private int ID;
    private double startX, startY, endX, endY;

    public Zone(int name, double startX, double startY, double endX, double endY) {
        this.ID = name;
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public int getID() { return ID; }
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public double getEndX() { return endX; }
    public double getEndY() { return endY; }
}