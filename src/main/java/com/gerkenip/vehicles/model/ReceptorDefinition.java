package com.gerkenip.vehicles.model;

/**
 * Template for creating receptor instances.
 * Immutable configuration used by Species to instantiate receptors for vehicles.
 */
public class ReceptorDefinition {
    private String id;
    private double angleFrom;
    private double angleTo;
    private double maxRange;
    private double sensitivity;
    private String colorFilter;
    private double threshold;

    public ReceptorDefinition() {
    }

    public ReceptorDefinition(String id, double angleFrom, double angleTo, double maxRange,
                             double sensitivity, String colorFilter, double threshold) {
        this.id = id;
        this.angleFrom = angleFrom;
        this.angleTo = angleTo;
        this.maxRange = maxRange;
        this.sensitivity = sensitivity;
        this.colorFilter = colorFilter;
        this.threshold = threshold;
    }

    /**
     * Creates a Receptor instance from this definition.
     */
    public Receptor createReceptor() {
        return new Receptor(id, angleFrom, angleTo, maxRange, sensitivity, colorFilter, threshold);
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getAngleFrom() {
        return angleFrom;
    }

    public void setAngleFrom(double angleFrom) {
        this.angleFrom = angleFrom;
    }

    public double getAngleTo() {
        return angleTo;
    }

    public void setAngleTo(double angleTo) {
        this.angleTo = angleTo;
    }

    public double getMaxRange() {
        return maxRange;
    }

    public void setMaxRange(double maxRange) {
        this.maxRange = maxRange;
    }

    public double getSensitivity() {
        return sensitivity;
    }

    public void setSensitivity(double sensitivity) {
        this.sensitivity = sensitivity;
    }

    public String getColorFilter() {
        return colorFilter;
    }

    public void setColorFilter(String colorFilter) {
        this.colorFilter = colorFilter;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }
}
