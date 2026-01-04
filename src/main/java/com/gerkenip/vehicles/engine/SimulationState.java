package com.gerkenip.vehicles.engine;

import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight snapshot of simulation state for broadcasting.
 * Does not include full neural network details.
 */
public class SimulationState {
    private String simulationId;
    private long tick;
    private List<VehicleState> vehicles;
    private List<StaticObjectState> staticObjects;

    public SimulationState() {
        this.vehicles = new ArrayList<>();
        this.staticObjects = new ArrayList<>();
    }

    // Getters and setters
    public String getSimulationId() {
        return simulationId;
    }

    public void setSimulationId(String simulationId) {
        this.simulationId = simulationId;
    }

    public long getTick() {
        return tick;
    }

    public void setTick(long tick) {
        this.tick = tick;
    }

    public List<VehicleState> getVehicles() {
        return vehicles;
    }

    public void setVehicles(List<VehicleState> vehicles) {
        this.vehicles = vehicles;
    }

    public List<StaticObjectState> getStaticObjects() {
        return staticObjects;
    }

    public void setStaticObjects(List<StaticObjectState> staticObjects) {
        this.staticObjects = staticObjects;
    }
}

/**
 * Vehicle state snapshot.
 */
class VehicleState {
    private String id;
    private double x;
    private double y;
    private double angle;
    private String colorName;
    private double brightness;

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

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
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
}

/**
 * Static object state snapshot.
 */
class StaticObjectState {
    private String id;
    private double x;
    private double y;
    private String colorName;
    private double brightness;
    private String type;  // "POINT" or "WALL"
    private Double x2;     // For walls
    private Double y2;     // For walls

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getX2() {
        return x2;
    }

    public void setX2(Double x2) {
        this.x2 = x2;
    }

    public Double getY2() {
        return y2;
    }

    public void setY2(Double y2) {
        this.y2 = y2;
    }
}
