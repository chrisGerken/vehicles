package com.gerkenip.vehicles.util;

/**
 * Exception thrown when an operation is not allowed in the current simulation state.
 * HTTP 409 Conflict.
 */
public class SimulationStateException extends SimulationException {
    public SimulationStateException(String message) {
        super(message, 409, "INVALID_STATE");
    }

    public static SimulationStateException alreadyRunning(String simulationId) {
        return new SimulationStateException("Simulation " + simulationId + " is already running");
    }

    public static SimulationStateException notRunning(String simulationId) {
        return new SimulationStateException("Simulation " + simulationId + " is not running");
    }

    public static SimulationStateException mustBeStopped(String operation) {
        return new SimulationStateException("Simulation must be stopped for operation: " + operation);
    }
}
