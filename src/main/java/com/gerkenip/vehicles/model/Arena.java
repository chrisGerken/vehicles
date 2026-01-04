package com.gerkenip.vehicles.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Arena containing all simulation objects.
 */
public class Arena {
    private double width;
    private double height;
    private boolean wrapEastWest;   // Whether east/west edges wrap (toroidal)
    private boolean wrapNorthSouth; // Whether north/south edges wrap (toroidal)
    private String backgroundColor; // Background color (#RRGGBB format)

    // Collections
    private List<Vehicle> vehicles;
    private List<StaticSimulationObject> staticObjects;

    public Arena() {
        this.vehicles = new ArrayList<>();
        this.staticObjects = new ArrayList<>();
        this.backgroundColor = "#000000";  // Black default
    }

    public Arena(double width, double height, boolean wrapEastWest, boolean wrapNorthSouth) {
        this.width = width;
        this.height = height;
        this.wrapEastWest = wrapEastWest;
        this.wrapNorthSouth = wrapNorthSouth;
        this.vehicles = new ArrayList<>();
        this.staticObjects = new ArrayList<>();
        this.backgroundColor = "#000000";
    }

    /**
     * Normalizes position to handle wrapping.
     * Returns adjusted coordinates if wrapping is enabled.
     */
    public double[] normalizePosition(double x, double y) {
        double nx = x;
        double ny = y;

        if (wrapEastWest) {
            while (nx < 0) nx += width;
            while (nx >= width) nx -= width;
        }

        if (wrapNorthSouth) {
            while (ny < 0) ny += height;
            while (ny >= height) ny -= height;
        }

        return new double[]{nx, ny};
    }

    /**
     * Gets all objects within a radius of a point.
     * Simple brute-force implementation - can be optimized with spatial indexing later.
     */
    public List<SimulationObject> getObjectsInRadius(double x, double y, double radius) {
        List<SimulationObject> result = new ArrayList<>();

        for (Vehicle vehicle : vehicles) {
            double dist = Math.sqrt((vehicle.getX() - x) * (vehicle.getX() - x) +
                                   (vehicle.getY() - y) * (vehicle.getY() - y));
            if (dist <= radius) {
                result.add(vehicle);
            }
        }

        for (StaticSimulationObject obj : staticObjects) {
            double dist = Math.sqrt((obj.getX() - x) * (obj.getX() - x) +
                                   (obj.getY() - y) * (obj.getY() - y));
            if (dist <= radius) {
                result.add(obj);
            }
        }

        return result;
    }

    /**
     * Checks if vehicle hits wall or exceeds boundaries.
     */
    public boolean checkBoundaryCollision(Vehicle vehicle) {
        double vx = vehicle.getX();
        double vy = vehicle.getY();

        // Check if outside bounds (when wrapping is disabled)
        if (!wrapEastWest) {
            if (vx < 0 || vx >= width) return true;
        }

        if (!wrapNorthSouth) {
            if (vy < 0 || vy >= height) return true;
        }

        return false;
    }

    /**
     * Adds a vehicle to the arena.
     */
    public void addVehicle(Vehicle vehicle) {
        vehicles.add(vehicle);
    }

    /**
     * Removes a vehicle from the arena.
     */
    public void removeVehicle(Vehicle vehicle) {
        vehicles.remove(vehicle);
    }

    /**
     * Adds a static object to the arena.
     */
    public void addStaticObject(StaticSimulationObject obj) {
        staticObjects.add(obj);
    }

    /**
     * Removes a static object from the arena.
     */
    public void removeStaticObject(StaticSimulationObject obj) {
        staticObjects.remove(obj);
    }

    // Getters and setters
    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public boolean isWrapEastWest() {
        return wrapEastWest;
    }

    public void setWrapEastWest(boolean wrapEastWest) {
        this.wrapEastWest = wrapEastWest;
    }

    public boolean isWrapNorthSouth() {
        return wrapNorthSouth;
    }

    public void setWrapNorthSouth(boolean wrapNorthSouth) {
        this.wrapNorthSouth = wrapNorthSouth;
    }

    public String getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(String backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public List<Vehicle> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<Vehicle> vehicles) {
        this.vehicles = vehicles;
    }

    public List<StaticSimulationObject> getStaticObjects() {
        return staticObjects;
    }

    public void setStaticObjects(List<StaticSimulationObject> staticObjects) {
        this.staticObjects = staticObjects;
    }

    @Override
    public String toString() {
        return "Arena{" + (int)width + "x" + (int)height +
               ", vehicles=" + vehicles.size() +
               ", objects=" + staticObjects.size() + "}";
    }
}
