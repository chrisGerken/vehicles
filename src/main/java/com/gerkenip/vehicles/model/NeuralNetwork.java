package com.gerkenip.vehicles.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Neural network composed of neurodes and connections.
 * Supports wave-based synchronous evaluation with cycles and self-loops.
 */
public class NeuralNetwork {
    private Map<String, Neurode> neurodes;  // neurodeId -> Neurode
    private List<Connection> connections;

    public NeuralNetwork() {
        this.neurodes = new HashMap<>();
        this.connections = new ArrayList<>();
    }

    /**
     * Adds a neurode to the network.
     */
    public void addNeurode(Neurode neurode) {
        neurodes.put(neurode.getId(), neurode);
    }

    /**
     * Adds a connection between neurodes.
     * Automatically updates inputConnections and outputConnections of the neurodes.
     */
    public void addConnection(Connection connection) {
        connections.add(connection);

        // Update neurode connection lists
        Neurode fromNeurode = neurodes.get(connection.getFromNeurodeId());
        Neurode toNeurode = neurodes.get(connection.getToNeurodeId());

        if (fromNeurode != null) {
            fromNeurode.addOutputConnection(connection);
        }
        if (toNeurode != null) {
            toNeurode.addInputConnection(connection);
        }
    }

    /**
     * Gets a neurode by ID.
     */
    public Neurode getNeurode(String id) {
        return neurodes.get(id);
    }

    /**
     * Gets all neurodes.
     */
    public Map<String, Neurode> getNeurodes() {
        return neurodes;
    }

    /**
     * Gets all connections.
     */
    public List<Connection> getConnections() {
        return connections;
    }

    /**
     * Gets all OUTPUT neurodes.
     */
    public List<Neurode> getOutputNeurodes() {
        List<Neurode> outputs = new ArrayList<>();
        for (Neurode neurode : neurodes.values()) {
            if (neurode.getType() == NeurodeType.OUTPUT) {
                outputs.add(neurode);
            }
        }
        return outputs;
    }

    /**
     * Gets all INPUT neurodes.
     */
    public List<Neurode> getInputNeurodes() {
        List<Neurode> inputs = new ArrayList<>();
        for (Neurode neurode : neurodes.values()) {
            if (neurode.getType() == NeurodeType.INPUT) {
                inputs.add(neurode);
            }
        }
        return inputs;
    }

    /**
     * Validates the neural network structure.
     * Checks for required output neurodes and valid connections.
     */
    public void validate() throws IllegalStateException {
        // Check for required output neurodes
        boolean hasLeftMotor = false;
        boolean hasRightMotor = false;

        for (Neurode neurode : neurodes.values()) {
            if (neurode.getType() == NeurodeType.OUTPUT) {
                String id = neurode.getId().toLowerCase();
                if (id.contains("left") && id.contains("motor")) {
                    hasLeftMotor = true;
                }
                if (id.contains("right") && id.contains("motor")) {
                    hasRightMotor = true;
                }
            }
        }

        if (!hasLeftMotor || !hasRightMotor) {
            throw new IllegalStateException("Neural network must have output_left_motor and output_right_motor neurodes");
        }

        // Check that all connections reference existing neurodes
        for (Connection conn : connections) {
            if (!neurodes.containsKey(conn.getFromNeurodeId())) {
                throw new IllegalStateException("Connection references non-existent from neurode: " + conn.getFromNeurodeId());
            }
            if (!neurodes.containsKey(conn.getToNeurodeId())) {
                throw new IllegalStateException("Connection references non-existent to neurode: " + conn.getToNeurodeId());
            }
        }
    }

    /**
     * Resets all neurodes in the network.
     */
    public void reset() {
        for (Neurode neurode : neurodes.values()) {
            neurode.reset();
        }
    }

    @Override
    public String toString() {
        return "NeuralNetwork{neurodes=" + neurodes.size() + ", connections=" + connections.size() + "}";
    }
}
