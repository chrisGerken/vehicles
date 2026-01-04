package com.gerkenip.vehicles.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Neurode in a neural network with wave-based synchronous evaluation.
 *
 * CRITICAL COMPONENT: Implements firing logic where all neurodes use the
 * previous tick's firing states to compute next tick's firing states.
 */
public class Neurode {
    private String id;
    private NeurodeType type;
    private int threshold;  // Number of firing excitatory connections required to fire

    // Wave-based state
    private boolean firedPreviousTick;  // State from previous clock tick
    private boolean willFireNextTick;   // Computed state for next clock tick

    // Connections (will be populated by NeuralNetwork)
    private List<Connection> inputConnections;
    private List<Connection> outputConnections;

    public Neurode() {
        this.inputConnections = new ArrayList<>();
        this.outputConnections = new ArrayList<>();
    }

    public Neurode(String id, NeurodeType type, int threshold) {
        this.id = id;
        this.type = type;
        this.threshold = threshold;
        this.firedPreviousTick = false;
        this.willFireNextTick = false;
        this.inputConnections = new ArrayList<>();
        this.outputConnections = new ArrayList<>();
    }

    /**
     * Advances tick: moves willFireNextTick to firedPreviousTick.
     * This creates the synchronized wave - all neurons update simultaneously.
     */
    public void advanceTick() {
        firedPreviousTick = willFireNextTick;
        willFireNextTick = false;  // Will be recomputed in evaluate()
    }

    /**
     * Evaluates this neurode based on input connections' previous tick states.
     *
     * Firing Logic:
     * 1. Count how many EXCITER input connections fired on previous tick
     * 2. Check if any INHIBITOR connections fired on previous tick
     * 3. If count >= threshold AND no inhibitors fired: willFireNextTick = true
     *
     * Note: This method should be called by NeuralNetworkEvaluator which has
     * access to all neurodes to check firedPreviousTick states.
     */
    public void evaluate(NeuralNetwork network) {
        // INPUT neurodes don't evaluate (set externally by receptors/constants)
        if (type == NeurodeType.INPUT) {
            return;
        }

        int excitersCount = 0;
        boolean inhibited = false;

        for (Connection conn : inputConnections) {
            Neurode fromNeurode = network.getNeurode(conn.getFromNeurodeId());
            if (fromNeurode == null) continue;

            if (fromNeurode.firedPreviousTick) {
                if (conn.getType() == ConnectionType.EXCITER) {
                    excitersCount++;
                } else if (conn.getType() == ConnectionType.INHIBITOR) {
                    inhibited = true;
                    break;  // One inhibitor is enough to prevent firing
                }
            }
        }

        // Determine if this neurode will fire next tick
        if (!inhibited && excitersCount >= threshold) {
            willFireNextTick = true;
        } else {
            willFireNextTick = false;
        }
    }

    /**
     * Resets this neurode's state.
     */
    public void reset() {
        firedPreviousTick = false;
        willFireNextTick = false;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public NeurodeType getType() {
        return type;
    }

    public void setType(NeurodeType type) {
        this.type = type;
    }

    public int getThreshold() {
        return threshold;
    }

    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    public boolean isFiredPreviousTick() {
        return firedPreviousTick;
    }

    public void setFiredPreviousTick(boolean firedPreviousTick) {
        this.firedPreviousTick = firedPreviousTick;
    }

    public boolean isWillFireNextTick() {
        return willFireNextTick;
    }

    public void setWillFireNextTick(boolean willFireNextTick) {
        this.willFireNextTick = willFireNextTick;
    }

    public List<Connection> getInputConnections() {
        return inputConnections;
    }

    public void setInputConnections(List<Connection> inputConnections) {
        this.inputConnections = inputConnections;
    }

    public List<Connection> getOutputConnections() {
        return outputConnections;
    }

    public void setOutputConnections(List<Connection> outputConnections) {
        this.outputConnections = outputConnections;
    }

    public void addInputConnection(Connection conn) {
        this.inputConnections.add(conn);
    }

    public void addOutputConnection(Connection conn) {
        this.outputConnections.add(conn);
    }

    @Override
    public String toString() {
        return "Neurode{" + id + " [" + type + ", threshold=" + threshold +
               ", prev=" + firedPreviousTick + ", next=" + willFireNextTick + "]}";
    }
}
