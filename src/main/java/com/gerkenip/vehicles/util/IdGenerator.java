package com.gerkenip.vehicles.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe ID generation for simulation entities.
 */
public class IdGenerator {
    private static final AtomicLong simulationCounter = new AtomicLong(1);
    private static final AtomicLong vehicleCounter = new AtomicLong(1);
    private static final AtomicLong speciesCounter = new AtomicLong(1);
    private static final AtomicLong objectCounter = new AtomicLong(1);
    private static final AtomicLong packageCounter = new AtomicLong(1);
    private static final AtomicLong neurodeCounter = new AtomicLong(1);
    private static final AtomicLong connectionCounter = new AtomicLong(1);

    /**
     * Generates a unique simulation ID.
     * Format: "sim_1", "sim_2", etc.
     */
    public static String generateSimulationId() {
        return "sim_" + simulationCounter.getAndIncrement();
    }

    /**
     * Generates a unique vehicle ID.
     * Format: "v_1", "v_2", etc.
     */
    public static String generateVehicleId() {
        return "v_" + vehicleCounter.getAndIncrement();
    }

    /**
     * Generates a unique species ID.
     * Format: "species_1", "species_2", etc.
     */
    public static String generateSpeciesId() {
        return "species_" + speciesCounter.getAndIncrement();
    }

    /**
     * Generates a unique object ID.
     * Format: "obj_1", "obj_2", etc.
     */
    public static String generateObjectId() {
        return "obj_" + objectCounter.getAndIncrement();
    }

    /**
     * Generates a unique package ID.
     * Format: "pkg_1", "pkg_2", etc.
     */
    public static String generatePackageId() {
        return "pkg_" + packageCounter.getAndIncrement();
    }

    /**
     * Generates a unique neurode ID.
     * Format: "n_1", "n_2", etc.
     */
    public static String generateNeurodeId() {
        return "n_" + neurodeCounter.getAndIncrement();
    }

    /**
     * Generates a unique connection ID.
     * Format: "c_1", "c_2", etc.
     */
    public static String generateConnectionId() {
        return "c_" + connectionCounter.getAndIncrement();
    }

    /**
     * Resets all counters (for testing).
     */
    public static void reset() {
        simulationCounter.set(1);
        vehicleCounter.set(1);
        speciesCounter.set(1);
        objectCounter.set(1);
        packageCounter.set(1);
        neurodeCounter.set(1);
        connectionCounter.set(1);
    }
}
