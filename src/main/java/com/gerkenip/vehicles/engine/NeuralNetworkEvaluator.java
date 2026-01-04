package com.gerkenip.vehicles.engine;

import com.gerkenip.vehicles.model.*;

import java.util.Map;

/**
 * Neural network evaluator using wave-based synchronous evaluation.
 *
 * MOST CRITICAL COMPONENT: This implements the wave-based synchronous evaluation
 * where all neurodes use the previous tick's firing states to compute next tick's
 * firing states. This creates synchronized waves of activation propagating through
 * the network.
 *
 * Key principle: All neurodes update simultaneously using previous tick states.
 * No iteration loops or convergence detection needed. Cycles and self-loops work
 * naturally with 1-tick delay per cycle.
 */
public class NeuralNetworkEvaluator {

    /**
     * Evaluates the neural network for one tick.
     *
     * Algorithm:
     * 1. Set INPUT neurodes from receptor inputs
     * 2. Advance all neurodes (synchronous state transition: willFireNextTick → firedPreviousTick)
     * 3. Evaluate all HIDDEN and OUTPUT neurodes based on previous tick states
     * 4. Return motor speeds from OUTPUT neurodes
     *
     * @param network The neural network to evaluate
     * @param receptorInputs Map of receptor IDs to their firing states
     * @return Array of [leftMotorSpeed, rightMotorSpeed] (0.0 or 1.0)
     */
    public double[] evaluate(NeuralNetwork network, Map<String, Boolean> receptorInputs) {
        // Step 1: Set INPUT neurodes from receptors
        setInputNeurodes(network, receptorInputs);

        // Step 2: Advance tick - synchronous state transition for ALL neurodes
        // This creates the wave: all neurons move willFireNextTick → firedPreviousTick simultaneously
        advanceAllNeurodes(network);

        // Step 3: Evaluate all HIDDEN and OUTPUT neurodes based on previous tick states
        evaluateAllNeurodes(network);

        // Step 4: Read motor speeds from OUTPUT neurodes
        return readOutputs(network);
    }

    /**
     * Sets INPUT neurodes based on receptor firing states.
     * INPUT neurodes are set externally and don't evaluate based on connections.
     */
    private void setInputNeurodes(NeuralNetwork network, Map<String, Boolean> receptorInputs) {
        for (Neurode neurode : network.getInputNeurodes()) {
            if (neurode.getType() == NeurodeType.INPUT) {
                String neurodeId = neurode.getId();

                // Check if this INPUT corresponds to a receptor
                Boolean firing = receptorInputs.get(neurodeId);

                if (firing != null) {
                    // Set willFireNextTick based on receptor state
                    neurode.setWillFireNextTick(firing);
                } else if (neurodeId.toLowerCase().contains("bias")) {
                    // Bias neurons always fire (threshold 0)
                    neurode.setWillFireNextTick(true);
                } else {
                    // Default: not firing
                    neurode.setWillFireNextTick(false);
                }
            }
        }
    }

    /**
     * Advances tick for ALL neurodes: moves willFireNextTick to firedPreviousTick.
     * This is the synchronous state transition that creates the wave effect.
     *
     * CRITICAL: All neurodes update simultaneously - this is what makes cycles work.
     */
    private void advanceAllNeurodes(NeuralNetwork network) {
        for (Neurode neurode : network.getNeurodes().values()) {
            neurode.advanceTick();
        }
    }

    /**
     * Evaluates all HIDDEN and OUTPUT neurodes based on previous tick states.
     * INPUT neurodes don't evaluate (they're set externally).
     */
    private void evaluateAllNeurodes(NeuralNetwork network) {
        for (Neurode neurode : network.getNeurodes().values()) {
            if (neurode.getType() != NeurodeType.INPUT) {
                // Evaluate this neurode based on its input connections' previous tick states
                neurode.evaluate(network);
            }
        }
    }

    /**
     * Reads motor speeds from OUTPUT neurodes.
     * Returns [leftMotorSpeed, rightMotorSpeed].
     *
     * Note: We read firedPreviousTick because we just advanced the tick.
     * The current tick's firing state is in firedPreviousTick.
     */
    private double[] readOutputs(NeuralNetwork network) {
        double leftMotorSpeed = 0.0;
        double rightMotorSpeed = 0.0;

        for (Neurode neurode : network.getOutputNeurodes()) {
            if (neurode.getType() == NeurodeType.OUTPUT) {
                String id = neurode.getId().toLowerCase();

                // Use firedPreviousTick because we just advanced the tick
                boolean fired = neurode.isFiredPreviousTick();

                if (id.contains("left") && id.contains("motor")) {
                    leftMotorSpeed = fired ? 1.0 : 0.0;
                } else if (id.contains("right") && id.contains("motor")) {
                    rightMotorSpeed = fired ? 1.0 : 0.0;
                }
            }
        }

        return new double[]{leftMotorSpeed, rightMotorSpeed};
    }

    /**
     * Resets all neurodes in the network to initial state.
     */
    public void reset(NeuralNetwork network) {
        network.reset();
    }
}
