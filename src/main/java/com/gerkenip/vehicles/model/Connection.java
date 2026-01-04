package com.gerkenip.vehicles.model;

/**
 * Connection between two neurodes in a neural network.
 */
public class Connection {
    private String id;
    private String fromNeurodeId;
    private String toNeurodeId;
    private ConnectionType type;
    private double weight;  // 0.0 to 1.0

    public Connection() {
    }

    public Connection(String id, String fromNeurodeId, String toNeurodeId, ConnectionType type, double weight) {
        this.id = id;
        this.fromNeurodeId = fromNeurodeId;
        this.toNeurodeId = toNeurodeId;
        this.type = type;
        this.weight = weight;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFromNeurodeId() {
        return fromNeurodeId;
    }

    public void setFromNeurodeId(String fromNeurodeId) {
        this.fromNeurodeId = fromNeurodeId;
    }

    public String getToNeurodeId() {
        return toNeurodeId;
    }

    public void setToNeurodeId(String toNeurodeId) {
        this.toNeurodeId = toNeurodeId;
    }

    public ConnectionType getType() {
        return type;
    }

    public void setType(ConnectionType type) {
        this.type = type;
    }

    public double getWeight() {
        return weight;
    }

    public void setWeight(double weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Connection{" + fromNeurodeId + " -> " + toNeurodeId + " [" + type + ", w=" + weight + "]}";
    }
}
