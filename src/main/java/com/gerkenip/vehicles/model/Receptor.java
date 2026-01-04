package com.gerkenip.vehicles.model;

/**
 * Receptor that accumulates light over time like a capacitor.
 *
 * CRITICAL COMPONENT: Implements capacitor-based light accumulation algorithm.
 * Detects brightness only from objects matching colorFilter.
 * Accumulates light over multiple ticks until threshold is reached.
 */
public class Receptor {
    private String id;
    private double angleFrom;  // Start angle relative to vehicle heading (radians)
    private double angleTo;    // End angle relative to vehicle heading (radians)
    private double maxRange;   // Maximum detection distance
    private double sensitivity; // Sensitivity multiplier
    private String colorFilter; // Only detects objects of this color

    // Capacitor state
    private double accumulatedLight;  // Current accumulated light (capacitor charge)
    private double threshold;         // Light threshold for firing
    private boolean willFireNextTick; // Whether receptor will fire on next clock tick

    public Receptor() {
    }

    public Receptor(String id, double angleFrom, double angleTo, double maxRange,
                    double sensitivity, String colorFilter, double threshold) {
        this.id = id;
        this.angleFrom = angleFrom;
        this.angleTo = angleTo;
        this.maxRange = maxRange;
        this.sensitivity = sensitivity;
        this.colorFilter = colorFilter;
        this.threshold = threshold;
        this.accumulatedLight = 0.0;
        this.willFireNextTick = false;
    }

    /**
     * Accumulates light from matching-color objects in the arena.
     *
     * Algorithm:
     * 1. For each SimulationObject in arena:
     *    a. Check if object.colorName matches this receptor's colorFilter
     *    b. Calculate relative angle from vehicle to object
     *    c. Check if angle is within [angleFrom, angleTo]
     *    d. If yes, calculate distance
     *    e. If distance < maxRange:
     *       contribution = object.brightness × (1 - distance/maxRange) × sensitivity
     *       accumulatedLight += contribution
     *
     * Note: This method is called once per tick before checkThreshold().
     * The actual implementation requires access to Vehicle and Arena,
     * which will be provided by the simulation engine.
     */
    public void accumulateLight(Vehicle vehicle, Arena arena) {
        // This will be called by the simulation engine during SENSE phase
        // For now, this is a placeholder - actual implementation will be in SimulationEngine
        // or we could implement it here if we pass the necessary objects
    }

    /**
     * Checks if threshold exceeded and sets willFireNextTick accordingly.
     *
     * Algorithm:
     * if accumulatedLight >= threshold:
     *     willFireNextTick = true
     *     accumulatedLight -= threshold  // Reduce charge by threshold amount
     * else:
     *     willFireNextTick = false
     *
     * This implements the capacitor behavior where charge accumulates
     * over multiple ticks and is partially discharged when firing.
     */
    public void checkThreshold() {
        if (accumulatedLight >= threshold) {
            willFireNextTick = true;
            accumulatedLight -= threshold;  // Partial discharge
        } else {
            willFireNextTick = false;
        }
    }

    /**
     * Resets fire state for new tick (called after neurode reads the value).
     */
    public void reset() {
        // Note: We don't reset accumulatedLight here - it persists across ticks (capacitor)
        // We only reset the firing flag after it's been read
        // Actually, based on the design, willFireNextTick is read by INPUT neurodes
        // and doesn't need to be reset here
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

    public double getAccumulatedLight() {
        return accumulatedLight;
    }

    public void setAccumulatedLight(double accumulatedLight) {
        this.accumulatedLight = accumulatedLight;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public boolean isWillFireNextTick() {
        return willFireNextTick;
    }

    public void setWillFireNextTick(boolean willFireNextTick) {
        this.willFireNextTick = willFireNextTick;
    }

    @Override
    public String toString() {
        return "Receptor{" + id + " [" + colorFilter + ", charge=" + accumulatedLight +
               ", threshold=" + threshold + ", fire=" + willFireNextTick + "]}";
    }
}
