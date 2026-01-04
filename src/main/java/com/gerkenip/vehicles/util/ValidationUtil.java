package com.gerkenip.vehicles.util;

import com.gerkenip.vehicles.model.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation utilities implementing rules from design.md.
 */
public class ValidationUtil {

    /**
     * Validates arena configuration.
     */
    public static void validateArena(Arena arena) {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        // Width and height must be between 100 and 10000
        if (arena.getWidth() < 100 || arena.getWidth() > 10000) {
            errors.add(new ValidationException.ValidationError("width", arena.getWidth(),
                    "Arena width must be between 100 and 10000 units"));
        }

        if (arena.getHeight() < 100 || arena.getHeight() > 10000) {
            errors.add(new ValidationException.ValidationError("height", arena.getHeight(),
                    "Arena height must be between 100 and 10000 units"));
        }

        // Background color must be valid hex
        if (arena.getBackgroundColor() != null && !isValidHexColor(arena.getBackgroundColor())) {
            errors.add(new ValidationException.ValidationError("backgroundColor", arena.getBackgroundColor(),
                    "Invalid color format. Use #RRGGBB"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Arena validation failed", errors);
        }
    }

    /**
     * Validates vehicle configuration.
     */
    public static void validateVehicle(Vehicle vehicle, Arena arena) {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        // Position must be within arena bounds
        if (vehicle.getX() < 0 || vehicle.getX() >= arena.getWidth()) {
            errors.add(new ValidationException.ValidationError("x", vehicle.getX(),
                    "Vehicle X position must be within arena bounds (0-" + arena.getWidth() + ")"));
        }

        if (vehicle.getY() < 0 || vehicle.getY() >= arena.getHeight()) {
            errors.add(new ValidationException.ValidationError("y", vehicle.getY(),
                    "Vehicle Y position must be within arena bounds (0-" + arena.getHeight() + ")"));
        }

        // Angle must be 0 to 2π
        if (vehicle.getAngle() < 0 || vehicle.getAngle() >= 2 * Math.PI) {
            errors.add(new ValidationException.ValidationError("angle", vehicle.getAngle(),
                    "Invalid angle, must be 0 to 2π radians"));
        }

        // Motor speeds must be -1.0 to 1.0
        if (vehicle.getLeftMotorSpeed() < -1.0 || vehicle.getLeftMotorSpeed() > 1.0) {
            errors.add(new ValidationException.ValidationError("leftMotorSpeed", vehicle.getLeftMotorSpeed(),
                    "Motor speed must be between -1.0 and 1.0"));
        }

        if (vehicle.getRightMotorSpeed() < -1.0 || vehicle.getRightMotorSpeed() > 1.0) {
            errors.add(new ValidationException.ValidationError("rightMotorSpeed", vehicle.getRightMotorSpeed(),
                    "Motor speed must be between -1.0 and 1.0"));
        }

        // Radius must be 1 to 100
        if (vehicle.getRadius() < 1 || vehicle.getRadius() > 100) {
            errors.add(new ValidationException.ValidationError("radius", vehicle.getRadius(),
                    "Vehicle radius must be between 1 and 100 units"));
        }

        // Wheel base must be 2 to 200
        if (vehicle.getWheelBase() < 2 || vehicle.getWheelBase() > 200) {
            errors.add(new ValidationException.ValidationError("wheelBase", vehicle.getWheelBase(),
                    "Wheel base must be between 2 and 200 units"));
        }

        // Max speed must be 1 to 1000
        if (vehicle.getMaxSpeed() < 1 || vehicle.getMaxSpeed() > 1000) {
            errors.add(new ValidationException.ValidationError("maxSpeed", vehicle.getMaxSpeed(),
                    "Max speed must be between 1 and 1000 units/second"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Vehicle validation failed", errors);
        }
    }

    /**
     * Validates species configuration.
     */
    public static void validateSpecies(Species species) {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        // Name is required and must be 1-100 characters
        if (species.getName() == null || species.getName().trim().isEmpty()) {
            errors.add(new ValidationException.ValidationError("name", species.getName(),
                    "Species name is required"));
        } else if (species.getName().length() > 100) {
            errors.add(new ValidationException.ValidationError("name", species.getName(),
                    "Species name must be 1-100 characters"));
        }

        // Receptors: 0-20 with unique IDs
        List<ReceptorDefinition> receptors = species.getReceptorDefinitions();
        if (receptors != null && receptors.size() > 20) {
            errors.add(new ValidationException.ValidationError("receptors", receptors.size(),
                    "Species can have 0-20 receptors"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Species validation failed", errors);
        }
    }

    /**
     * Validates receptor configuration.
     */
    public static void validateReceptor(Receptor receptor) {
        List<ValidationException.ValidationError> errors = new ArrayList<>();

        // Angles must be in range -2π to 2π
        if (receptor.getAngleFrom() < -2 * Math.PI || receptor.getAngleFrom() > 2 * Math.PI) {
            errors.add(new ValidationException.ValidationError("angleFrom", receptor.getAngleFrom(),
                    "Receptor angles must be between -2π and 2π"));
        }

        if (receptor.getAngleTo() < -2 * Math.PI || receptor.getAngleTo() > 2 * Math.PI) {
            errors.add(new ValidationException.ValidationError("angleTo", receptor.getAngleTo(),
                    "Receptor angles must be between -2π and 2π"));
        }

        // Max range must be 10 to 1000
        if (receptor.getMaxRange() < 10 || receptor.getMaxRange() > 1000) {
            errors.add(new ValidationException.ValidationError("maxRange", receptor.getMaxRange(),
                    "Receptor max range must be between 10 and 1000 units"));
        }

        // Sensitivity must be 0.0 to 10.0
        if (receptor.getSensitivity() < 0.0 || receptor.getSensitivity() > 10.0) {
            errors.add(new ValidationException.ValidationError("sensitivity", receptor.getSensitivity(),
                    "Receptor sensitivity must be between 0.0 and 10.0"));
        }

        if (!errors.isEmpty()) {
            throw new ValidationException("Receptor validation failed", errors);
        }
    }

    /**
     * Validates neural network structure.
     */
    public static void validateNeuralNetwork(NeuralNetwork network) {
        // This will call network.validate() which checks for required outputs
        try {
            network.validate();
        } catch (IllegalStateException e) {
            throw new ValidationException(e.getMessage());
        }
    }

    /**
     * Checks if a string is a valid hex color (#RRGGBB).
     */
    private static boolean isValidHexColor(String color) {
        if (color == null) return false;
        return color.matches("^#[0-9A-Fa-f]{6}$");
    }
}
