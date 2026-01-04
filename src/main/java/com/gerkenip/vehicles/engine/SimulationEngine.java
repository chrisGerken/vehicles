package com.gerkenip.vehicles.engine;

import com.gerkenip.vehicles.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main simulation engine orchestrating the tick cycle.
 *
 * CRITICAL COMPONENT: Implements the correct tick sequence from design.md:
 * 1. SENSE: receptors.accumulateLight() + checkThreshold()
 * 2. ADVANCE: all neurodes.advanceTick()
 * 3. THINK: NeuralNetworkEvaluator.evaluate()
 * 4. ACT: PhysicsEngine.updatePosition()
 * 5. BROADCAST: notify listeners
 */
public class SimulationEngine {
    private static final Logger logger = LoggerFactory.getLogger(SimulationEngine.class);

    private final Simulation simulation;
    private final NeuralNetworkEvaluator neuralEvaluator;
    private final PhysicsEngine physicsEngine;

    private Thread simulationThread;
    private volatile boolean running;

    // State listeners for broadcasting updates
    private final List<StateListener> stateListeners;

    public SimulationEngine(Simulation simulation) {
        this.simulation = simulation;
        this.neuralEvaluator = new NeuralNetworkEvaluator();
        this.physicsEngine = new PhysicsEngine();
        this.stateListeners = new ArrayList<>();
        this.running = false;
    }

    /**
     * Starts the simulation in a background thread.
     */
    public synchronized void start() {
        if (running) {
            logger.warn("Simulation {} is already running", simulation.getId());
            return;
        }

        running = true;
        simulation.setRunning(true);

        simulationThread = new Thread(() -> {
            logger.info("Starting simulation {} at tick {}", simulation.getId(), simulation.getCurrentTick());

            while (running) {
                try {
                    executeTick();

                    // Sleep to maintain target ticks per second
                    if (simulation.getTicksPerSecond() > 0) {
                        long sleepMs = 1000L / simulation.getTicksPerSecond();
                        Thread.sleep(sleepMs);
                    }
                } catch (InterruptedException e) {
                    logger.info("Simulation {} interrupted", simulation.getId());
                    break;
                } catch (Exception e) {
                    logger.error("Error in simulation {} tick {}", simulation.getId(), simulation.getCurrentTick(), e);
                }
            }

            logger.info("Simulation {} stopped at tick {}", simulation.getId(), simulation.getCurrentTick());
        }, "SimulationEngine-" + simulation.getId());

        simulationThread.start();
    }

    /**
     * Stops the simulation.
     */
    public synchronized void stop() {
        if (!running) {
            logger.warn("Simulation {} is not running", simulation.getId());
            return;
        }

        running = false;
        simulation.setRunning(false);

        if (simulationThread != null) {
            try {
                simulationThread.join(5000);  // Wait up to 5 seconds
            } catch (InterruptedException e) {
                logger.warn("Interrupted while stopping simulation {}", simulation.getId());
            }
        }

        logger.info("Simulation {} stopped", simulation.getId());
    }

    /**
     * Executes a single tick (step).
     * Can be called directly for step-through debugging.
     */
    public synchronized void step() {
        executeTick();
    }

    /**
     * Executes one complete tick cycle.
     *
     * CRITICAL SEQUENCE:
     * 1. SENSE PHASE: receptors accumulate light
     * 2. THINK PHASE: neural networks evaluate
     * 3. ACT PHASE: physics updates positions
     * 4. BROADCAST PHASE: notify listeners
     */
    private void executeTick() {
        Arena arena = simulation.getArena();
        if (arena == null) {
            return;
        }

        List<Vehicle> vehicles = new ArrayList<>(arena.getVehicles());  // Copy to avoid concurrent modification

        // SENSE PHASE: All vehicles sense their environment
        for (Vehicle vehicle : vehicles) {
            // Receptors accumulate light and check thresholds
            Map<String, Boolean> sensorInputs = vehicle.sense(arena);

            // Evaluate neural network
            if (vehicle.getNeuralNetwork() != null) {
                double[] motorSpeeds = neuralEvaluator.evaluate(vehicle.getNeuralNetwork(), sensorInputs);
                vehicle.setLeftMotorSpeed(motorSpeeds[0]);
                vehicle.setRightMotorSpeed(motorSpeeds[1]);
            }
        }

        // ACT PHASE: All vehicles update positions
        List<Vehicle> toRemove = new ArrayList<>();
        for (Vehicle vehicle : vehicles) {
            // Update position based on motor speeds
            physicsEngine.updateVehiclePosition(vehicle, simulation.getDeltaTime());

            // Apply wrapping if enabled
            physicsEngine.applyWrapping(vehicle, arena);

            // Check for collisions
            if (physicsEngine.detectCollision(vehicle, arena, simulation.getCollisionBehavior())) {
                boolean shouldRemove = physicsEngine.handleCollision(vehicle, simulation.getCollisionBehavior());
                if (shouldRemove) {
                    toRemove.add(vehicle);
                }
            }
        }

        // Remove broken vehicles
        for (Vehicle vehicle : toRemove) {
            arena.removeVehicle(vehicle);
            logger.debug("Vehicle {} removed due to collision", vehicle.getId());
        }

        // Increment tick
        simulation.tick();

        // BROADCAST PHASE: Notify listeners
        broadcastState();
    }

    /**
     * Broadcasts current state to all listeners.
     */
    private void broadcastState() {
        SimulationState state = createStateSnapshot();

        for (StateListener listener : stateListeners) {
            try {
                listener.onStateUpdate(simulation.getId(), state);
            } catch (Exception e) {
                logger.error("Error broadcasting to listener", e);
            }
        }
    }

    /**
     * Creates a lightweight snapshot of current simulation state.
     */
    private SimulationState createStateSnapshot() {
        SimulationState state = new SimulationState();
        state.setSimulationId(simulation.getId());
        state.setTick(simulation.getCurrentTick());

        Arena arena = simulation.getArena();
        if (arena != null) {
            // Copy vehicle states
            List<VehicleState> vehicleStates = new ArrayList<>();
            for (Vehicle v : arena.getVehicles()) {
                VehicleState vs = new VehicleState();
                vs.setId(v.getId());
                vs.setX(v.getX());
                vs.setY(v.getY());
                vs.setAngle(v.getAngle());
                vs.setColorName(v.getColorName());
                vs.setBrightness(v.getBrightness());
                vehicleStates.add(vs);
            }
            state.setVehicles(vehicleStates);

            // Copy static object states
            List<StaticObjectState> objectStates = new ArrayList<>();
            for (StaticSimulationObject obj : arena.getStaticObjects()) {
                StaticObjectState os = new StaticObjectState();
                os.setId(obj.getId());
                os.setX(obj.getX());
                os.setY(obj.getY());
                os.setColorName(obj.getColorName());
                os.setBrightness(obj.getBrightness());
                os.setType(obj.getType().toString());
                if (obj.getType() == ObjectType.WALL) {
                    os.setX2(obj.getX2());
                    os.setY2(obj.getY2());
                }
                objectStates.add(os);
            }
            state.setStaticObjects(objectStates);
        }

        return state;
    }

    /**
     * Adds a state listener for broadcasts.
     */
    public void addStateListener(StateListener listener) {
        stateListeners.add(listener);
    }

    /**
     * Removes a state listener.
     */
    public void removeStateListener(StateListener listener) {
        stateListeners.remove(listener);
    }

    /**
     * Gets the simulation.
     */
    public Simulation getSimulation() {
        return simulation;
    }

    /**
     * Checks if simulation is running.
     */
    public boolean isRunning() {
        return running;
    }
}
