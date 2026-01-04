package com.gerkenip.vehicles.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vehicle that moves in the arena controlled by neural network.
 */
public class Vehicle extends SimulationObject {
    private double angle;  // Heading in radians (0 to 2π)
    private double leftMotorSpeed;   // Left wheel speed (-1.0 to 1.0)
    private double rightMotorSpeed;  // Right wheel speed (-1.0 to 1.0)
    private double wheelBase;  // Distance between left and right wheels
    private double maxSpeed;   // Maximum forward speed

    // References
    private Species species;  // Reference to species definition
    private List<Receptor> receptors;  // Sensor array
    private NeuralNetwork neuralNetwork;  // Instance of neural network

    public Vehicle() {
        super();
        this.receptors = new ArrayList<>();
    }

    public Vehicle(String id, double x, double y, String colorName, double brightness, double radius) {
        super(id, x, y, colorName, brightness, radius);
        this.angle = 0.0;
        this.leftMotorSpeed = 0.0;
        this.rightMotorSpeed = 0.0;
        this.receptors = new ArrayList<>();
    }

    /**
     * Senses the environment: all receptors accumulate light.
     * Returns a map of receptor IDs to their firing states.
     */
    public Map<String, Boolean> sense(Arena arena) {
        Map<String, Boolean> sensorInputs = new HashMap<>();

        // Each receptor accumulates light from arena
        for (Receptor receptor : receptors) {
            // Accumulate light from all objects in arena
            accumulateLightForReceptor(receptor, arena);

            // Check if threshold exceeded
            receptor.checkThreshold();

            // Store firing state
            sensorInputs.put(receptor.getId(), receptor.isWillFireNextTick());
        }

        return sensorInputs;
    }

    /**
     * Accumulates light for a single receptor from all objects in arena.
     * Implements the capacitor algorithm from design.md.
     */
    private void accumulateLightForReceptor(Receptor receptor, Arena arena) {
        // Get all objects in arena
        List<SimulationObject> allObjects = new ArrayList<>();
        allObjects.addAll(arena.getStaticObjects());
        allObjects.addAll(arena.getVehicles());

        for (SimulationObject obj : allObjects) {
            // Skip self
            if (obj == this) continue;

            // Check color filter
            if (!receptor.getColorFilter().equals(obj.getColorName())) {
                continue;
            }

            // Calculate relative angle from vehicle to object
            double dx = obj.getX() - this.x;
            double dy = obj.getY() - this.y;
            double absoluteAngle = Math.atan2(dy, dx);

            // Convert to vehicle's reference frame
            double relativeAngle = absoluteAngle - this.angle;

            // Normalize to [-π, π]
            while (relativeAngle > Math.PI) relativeAngle -= 2 * Math.PI;
            while (relativeAngle < -Math.PI) relativeAngle += 2 * Math.PI;

            // Check if angle is within receptor's field of view
            if (!isAngleBetween(relativeAngle, receptor.getAngleFrom(), receptor.getAngleTo())) {
                continue;
            }

            // Calculate distance
            double distance = this.distanceTo(obj);

            // Check if within max range
            if (distance < receptor.getMaxRange() && distance > 0) {
                // Calculate contribution
                double contribution = obj.getBrightness() *
                                    (1.0 - distance / receptor.getMaxRange()) *
                                    receptor.getSensitivity();

                // Accumulate light
                receptor.setAccumulatedLight(receptor.getAccumulatedLight() + contribution);
            }
        }
    }

    /**
     * Checks if an angle is between fromAngle and toAngle (handles wraparound).
     * This is a simple implementation - will be replaced by MathUtil later.
     */
    private boolean isAngleBetween(double angle, double fromAngle, double toAngle) {
        // Normalize all angles to [-π, π]
        angle = normalizeAngle(angle);
        fromAngle = normalizeAngle(fromAngle);
        toAngle = normalizeAngle(toAngle);

        if (fromAngle <= toAngle) {
            return angle >= fromAngle && angle <= toAngle;
        } else {
            // Wraparound case
            return angle >= fromAngle || angle <= toAngle;
        }
    }

    /**
     * Normalizes angle to [-π, π].
     */
    private double normalizeAngle(double angle) {
        while (angle > Math.PI) angle -= 2 * Math.PI;
        while (angle < -Math.PI) angle += 2 * Math.PI;
        return angle;
    }

    /**
     * Updates motor speeds from neural network output neurodes.
     * Called after neural network evaluation.
     */
    public void updateMotorSpeeds() {
        if (neuralNetwork == null) return;

        // Find output neurodes
        for (Neurode neurode : neuralNetwork.getOutputNeurodes()) {
            String id = neurode.getId().toLowerCase();
            if (id.contains("left") && id.contains("motor")) {
                leftMotorSpeed = neurode.isFiredPreviousTick() ? 1.0 : 0.0;
            } else if (id.contains("right") && id.contains("motor")) {
                rightMotorSpeed = neurode.isFiredPreviousTick() ? 1.0 : 0.0;
            }
        }
    }

    /**
     * Updates position based on differential drive physics.
     * Will be called by PhysicsEngine.
     */
    public void updatePosition(double deltaTime) {
        // Differential drive kinematics
        double vL = leftMotorSpeed * maxSpeed;
        double vR = rightMotorSpeed * maxSpeed;

        double linearVelocity = (vL + vR) / 2.0;
        double angularVelocity = (vR - vL) / wheelBase;

        // Update angle and position
        angle += angularVelocity * deltaTime;
        x += linearVelocity * Math.cos(angle) * deltaTime;
        y += linearVelocity * Math.sin(angle) * deltaTime;

        // Normalize angle to [0, 2π]
        while (angle < 0) angle += 2 * Math.PI;
        while (angle >= 2 * Math.PI) angle -= 2 * Math.PI;
    }

    // Getters and setters
    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public double getLeftMotorSpeed() {
        return leftMotorSpeed;
    }

    public void setLeftMotorSpeed(double leftMotorSpeed) {
        this.leftMotorSpeed = leftMotorSpeed;
    }

    public double getRightMotorSpeed() {
        return rightMotorSpeed;
    }

    public void setRightMotorSpeed(double rightMotorSpeed) {
        this.rightMotorSpeed = rightMotorSpeed;
    }

    public double getWheelBase() {
        return wheelBase;
    }

    public void setWheelBase(double wheelBase) {
        this.wheelBase = wheelBase;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public Species getSpecies() {
        return species;
    }

    public void setSpecies(Species species) {
        this.species = species;
    }

    public List<Receptor> getReceptors() {
        return receptors;
    }

    public void setReceptors(List<Receptor> receptors) {
        this.receptors = receptors;
    }

    public NeuralNetwork getNeuralNetwork() {
        return neuralNetwork;
    }

    public void setNeuralNetwork(NeuralNetwork neuralNetwork) {
        this.neuralNetwork = neuralNetwork;
    }

    @Override
    public String toString() {
        return "Vehicle{" + id + " at (" + (int)x + "," + (int)y + ") angle=" + String.format("%.2f", angle) +
               " motors=[" + String.format("%.2f", leftMotorSpeed) + "," + String.format("%.2f", rightMotorSpeed) + "]}";
    }
}
