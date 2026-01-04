package com.gerkenip.vehicles.service;

import com.gerkenip.vehicles.engine.SimulationEngine;
import com.gerkenip.vehicles.model.*;
import com.gerkenip.vehicles.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer for simulation management.
 * Handles simulation lifecycle and orchestrates with SimulationEngine.
 */
public class SimulationService {
    private static final Logger logger = LoggerFactory.getLogger(SimulationService.class);

    // Singleton instance
    private static final SimulationService INSTANCE = new SimulationService();

    // Storage
    private final Map<String, Simulation> simulations;
    private final Map<String, SimulationEngine> engines;

    private SimulationService() {
        this.simulations = new ConcurrentHashMap<>();
        this.engines = new ConcurrentHashMap<>();
    }

    public static SimulationService getInstance() {
        return INSTANCE;
    }

    /**
     * Creates a new simulation.
     */
    public Simulation createSimulation(String name, Arena arena) {
        String id = IdGenerator.generateSimulationId();

        Simulation simulation = new Simulation(id, name);
        simulation.setArena(arena);

        // Validate
        ValidationUtil.validateArena(arena);

        // Create default colors
        simulation.addColor(new ColorDefinition("white", 255, 255, 255));
        simulation.addColor(new ColorDefinition("red", 255, 0, 0));
        simulation.addColor(new ColorDefinition("blue", 0, 0, 255));
        simulation.addColor(new ColorDefinition("green", 0, 255, 0));

        // Create engine
        SimulationEngine engine = new SimulationEngine(simulation);

        // Store
        simulations.put(id, simulation);
        engines.put(id, engine);

        logger.info("Created simulation: {} [{}]", name, id);

        return simulation;
    }

    /**
     * Gets a simulation by ID.
     */
    public Simulation getSimulation(String id) {
        Simulation sim = simulations.get(id);
        if (sim == null) {
            throw new ResourceNotFoundException("Simulation", id);
        }
        return sim;
    }

    /**
     * Gets a simulation engine by ID.
     */
    public SimulationEngine getEngine(String id) {
        SimulationEngine engine = engines.get(id);
        if (engine == null) {
            throw new ResourceNotFoundException("Simulation engine", id);
        }
        return engine;
    }

    /**
     * Lists all simulations.
     */
    public Map<String, Simulation> listSimulations() {
        return new HashMap<>(simulations);
    }

    /**
     * Starts a simulation.
     */
    public void startSimulation(String id) {
        SimulationEngine engine = getEngine(id);

        if (engine.isRunning()) {
            throw SimulationStateException.alreadyRunning(id);
        }

        engine.start();
        logger.info("Started simulation: {}", id);
    }

    /**
     * Stops a simulation.
     */
    public void stopSimulation(String id) {
        SimulationEngine engine = getEngine(id);

        if (!engine.isRunning()) {
            throw SimulationStateException.notRunning(id);
        }

        engine.stop();
        logger.info("Stopped simulation: {}", id);
    }

    /**
     * Steps a simulation (single tick).
     */
    public void stepSimulation(String id) {
        SimulationEngine engine = getEngine(id);
        engine.step();
        logger.debug("Stepped simulation: {} to tick {}", id, engine.getSimulation().getCurrentTick());
    }

    /**
     * Resets a simulation.
     */
    public void resetSimulation(String id) {
        SimulationEngine engine = getEngine(id);

        if (engine.isRunning()) {
            throw SimulationStateException.mustBeStopped("reset");
        }

        engine.getSimulation().reset();
        logger.info("Reset simulation: {}", id);
    }

    /**
     * Deletes a simulation.
     */
    public void deleteSimulation(String id) {
        SimulationEngine engine = engines.get(id);

        if (engine != null && engine.isRunning()) {
            throw SimulationStateException.mustBeStopped("delete");
        }

        simulations.remove(id);
        engines.remove(id);

        logger.info("Deleted simulation: {}", id);
    }

    /**
     * Gets simulation status.
     */
    public Map<String, Object> getStatus(String id) {
        Simulation sim = getSimulation(id);
        SimulationEngine engine = getEngine(id);

        Map<String, Object> status = new HashMap<>();
        status.put("id", sim.getId());
        status.put("name", sim.getName());
        status.put("running", engine.isRunning());
        status.put("tick", sim.getCurrentTick());
        status.put("vehicleCount", sim.getArena() != null ? sim.getArena().getVehicles().size() : 0);
        status.put("ticksPerSecond", sim.getTicksPerSecond());
        status.put("threadCount", sim.getThreadCount());

        return status;
    }

    /**
     * Adds a vehicle to a simulation.
     */
    public Vehicle addVehicle(String simulationId, String speciesId, double x, double y, double angle) {
        Simulation sim = getSimulation(simulationId);

        Species species = sim.getSpecies(speciesId);
        if (species == null) {
            throw new ResourceNotFoundException("Species", speciesId);
        }

        String vehicleId = IdGenerator.generateVehicleId();
        Vehicle vehicle = species.createVehicle(vehicleId, x, y, angle);

        // Validate
        ValidationUtil.validateVehicle(vehicle, sim.getArena());

        sim.getArena().addVehicle(vehicle);

        logger.info("Added vehicle {} to simulation {}", vehicleId, simulationId);

        return vehicle;
    }

    /**
     * Adds a species to a simulation.
     */
    public void addSpecies(String simulationId, Species species) {
        Simulation sim = getSimulation(simulationId);

        // Validate
        ValidationUtil.validateSpecies(species);
        ValidationUtil.validateNeuralNetwork(species.getNeuralNetworkTemplate());

        sim.addSpecies(species);

        logger.info("Added species {} to simulation {}", species.getId(), simulationId);
    }

    /**
     * Adds a static object to a simulation.
     */
    public StaticSimulationObject addStaticObject(String simulationId, StaticSimulationObject object) {
        Simulation sim = getSimulation(simulationId);

        String id = IdGenerator.generateObjectId();
        object.setId(id);

        sim.getArena().addStaticObject(object);

        logger.info("Added static object {} to simulation {}", id, simulationId);

        return object;
    }
}
