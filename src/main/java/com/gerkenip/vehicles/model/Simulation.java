package com.gerkenip.vehicles.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level simulation container.
 */
public class Simulation {
    private String id;
    private String name;

    // Simulation components
    private Arena arena;
    private Map<String, ColorDefinition> colorDefinitions;  // Named colors
    private List<Species> species;  // Species defined in simulation

    // Configuration
    private CollisionBehavior collisionBehavior;
    private int threadCount;        // Number of threads for parallel processing
    private int ticksPerSecond;     // Target simulation speed
    private double deltaTime;       // Time per tick in simulation units

    // State
    private long currentTick;       // Current simulation tick
    private boolean running;        // Whether simulation is currently running

    public Simulation() {
        this.colorDefinitions = new HashMap<>();
        this.species = new ArrayList<>();
        this.collisionBehavior = new CollisionBehavior();
        this.threadCount = Runtime.getRuntime().availableProcessors();
        this.ticksPerSecond = 30;
        this.deltaTime = 0.1;
        this.currentTick = 0;
        this.running = false;
    }

    public Simulation(String id, String name) {
        this();
        this.id = id;
        this.name = name;
    }

    /**
     * Adds a species to this simulation.
     */
    public void addSpecies(Species spec) {
        species.add(spec);
    }

    /**
     * Gets a species by ID.
     */
    public Species getSpecies(String speciesId) {
        for (Species s : species) {
            if (s.getId().equals(speciesId)) {
                return s;
            }
        }
        return null;
    }

    /**
     * Adds a color definition.
     */
    public void addColor(ColorDefinition color) {
        colorDefinitions.put(color.getName(), color);
    }

    /**
     * Gets a color by name.
     */
    public ColorDefinition getColor(String colorName) {
        return colorDefinitions.get(colorName);
    }

    /**
     * Increments the current tick.
     */
    public void tick() {
        currentTick++;
    }

    /**
     * Resets the simulation to initial state.
     */
    public void reset() {
        currentTick = 0;
        running = false;
        if (arena != null) {
            arena.getVehicles().clear();
        }
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Arena getArena() {
        return arena;
    }

    public void setArena(Arena arena) {
        this.arena = arena;
    }

    public Map<String, ColorDefinition> getColorDefinitions() {
        return colorDefinitions;
    }

    public void setColorDefinitions(Map<String, ColorDefinition> colorDefinitions) {
        this.colorDefinitions = colorDefinitions;
    }

    public List<Species> getSpecies() {
        return species;
    }

    public void setSpecies(List<Species> species) {
        this.species = species;
    }

    public CollisionBehavior getCollisionBehavior() {
        return collisionBehavior;
    }

    public void setCollisionBehavior(CollisionBehavior collisionBehavior) {
        this.collisionBehavior = collisionBehavior;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public int getTicksPerSecond() {
        return ticksPerSecond;
    }

    public void setTicksPerSecond(int ticksPerSecond) {
        this.ticksPerSecond = ticksPerSecond;
    }

    public double getDeltaTime() {
        return deltaTime;
    }

    public void setDeltaTime(double deltaTime) {
        this.deltaTime = deltaTime;
    }

    public long getCurrentTick() {
        return currentTick;
    }

    public void setCurrentTick(long currentTick) {
        this.currentTick = currentTick;
    }

    public boolean isRunning() {
        return running;
    }

    public void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public String toString() {
        return "Simulation{" + name + " [" + id + "], tick=" + currentTick +
               ", running=" + running + ", vehicles=" + (arena != null ? arena.getVehicles().size() : 0) + "}";
    }
}
