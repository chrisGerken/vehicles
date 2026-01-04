package com.gerkenip.vehicles.engine;

/**
 * Listener interface for simulation state updates.
 * Implemented by components that need to receive state broadcasts (e.g., WebSocket).
 */
public interface StateListener {
    /**
     * Called when simulation state is updated.
     *
     * @param simulationId The ID of the simulation
     * @param state The current state snapshot
     */
    void onStateUpdate(String simulationId, SimulationState state);
}
