package com.gerkenip.vehicles.model;

/**
 * Abstract base class for all objects in the simulation arena.
 */
public abstract class SimulationObject {
    protected String id;
    protected double x;
    protected double y;
    protected String colorName;  // Reference to named color definition
    protected double brightness; // Brightness level (0.0 to 1.0)
    protected double radius;     // Collision radius (0 for point sources)

    public SimulationObject() {
    }

    public SimulationObject(String id, double x, double y, String colorName, double brightness, double radius) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.colorName = colorName;
        this.brightness = brightness;
        this.radius = radius;
    }

    /**
     * Calculate distance to another simulation object.
     */
    public double distanceTo(SimulationObject other) {
        double dx = this.x - other.x;
        double dy = this.y - other.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Calculate distance to a point.
     */
    public double distanceTo(double px, double py) {
        double dx = this.x - px;
        double dy = this.y - py;
        return Math.sqrt(dx * dx + dy * dy);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public double getBrightness() {
        return brightness;
    }

    public void setBrightness(double brightness) {
        this.brightness = brightness;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }
}
