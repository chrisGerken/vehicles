package com.gerkenip.vehicles.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for collision behavior in the simulation.
 */
public class CollisionBehavior {
    private CollisionMode defaultBehavior;  // Default collision handling
    private Map<String, CollisionMode> colorSpecificBehaviors;  // Per-color collision rules

    public CollisionBehavior() {
        this.defaultBehavior = CollisionMode.BOUNCE;
        this.colorSpecificBehaviors = new HashMap<>();
    }

    public CollisionBehavior(CollisionMode defaultBehavior) {
        this.defaultBehavior = defaultBehavior;
        this.colorSpecificBehaviors = new HashMap<>();
    }

    /**
     * Gets collision mode for a specific color.
     * Returns color-specific behavior if set, otherwise default.
     */
    public CollisionMode getBehaviorForColor(String colorName) {
        return colorSpecificBehaviors.getOrDefault(colorName, defaultBehavior);
    }

    /**
     * Sets collision behavior for a specific color.
     */
    public void setColorBehavior(String colorName, CollisionMode mode) {
        colorSpecificBehaviors.put(colorName, mode);
    }

    // Getters and setters
    public CollisionMode getDefaultBehavior() {
        return defaultBehavior;
    }

    public void setDefaultBehavior(CollisionMode defaultBehavior) {
        this.defaultBehavior = defaultBehavior;
    }

    public Map<String, CollisionMode> getColorSpecificBehaviors() {
        return colorSpecificBehaviors;
    }

    public void setColorSpecificBehaviors(Map<String, CollisionMode> colorSpecificBehaviors) {
        this.colorSpecificBehaviors = colorSpecificBehaviors;
    }
}
