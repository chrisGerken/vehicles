package com.gerkenip.vehicles.util;

import com.gerkenip.vehicles.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * Utility to programmatically create species packages.
 * Creates the base package with all 6 Braitenberg species.
 */
public class PackageBuilder {

    public static void main(String[] args) {
        try {
            PackageBuilder builder = new PackageBuilder();
            SpeciesPackage basePackage = builder.createBasePackage();

            // Write to file
            String outputPath = "src/main/resources/packages/base.vsp";
            builder.writePackage(basePackage, outputPath);

            System.out.println("Successfully created base package at: " + outputPath);
            System.out.println("Package contains " + basePackage.getSpecies().size() + " species:");
            for (Species species : basePackage.getSpecies()) {
                System.out.println("  - " + species.getName() + " (ID: " + species.getId() + ")");
            }
        } catch (Exception e) {
            System.err.println("Error creating package: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Creates the base species package with all 6 Braitenberg vehicles.
     */
    public SpeciesPackage createBasePackage() {
        SpeciesPackage pkg = new SpeciesPackage();
        pkg.setId("base");
        pkg.setName("Base Braitenberg Species");
        pkg.setVersion("1.0.0");
        pkg.setDescription("Contains all 6 classic Braitenberg vehicle species demonstrating emergent behaviors");

        List<Species> species = new ArrayList<>();
        species.add(createPhototrope());
        species.add(createPhotophobe());
        species.add(createExplorer());
        species.add(createAggressive());
        species.add(createCoward());
        species.add(createParanoid());

        pkg.setSpecies(species);
        return pkg;
    }

    /**
     * Creates Phototrope species - approaches light sources.
     * Uses crossed excitatory connections (left sensor → right motor, right sensor → left motor).
     */
    private Species createPhototrope() {
        Species species = new Species();
        species.setId("phototrope");
        species.setName("Phototrope");
        species.setDescription("Approaches light sources using crossed excitatory connections");

        // Physics parameters
        species.setRadius(10.0);
        species.setWheelBase(15.0);
        species.setMaxSpeed(50.0);
        species.setColorName("blue");

        // Receptors: left and right sensors
        List<ReceptorDefinition> receptors = new ArrayList<>();

        ReceptorDefinition leftReceptor = new ReceptorDefinition();
        leftReceptor.setId("sensor_left");
        leftReceptor.setAngleFrom(Math.toRadians(45));
        leftReceptor.setAngleTo(Math.toRadians(135));
        leftReceptor.setMaxRange(200.0);
        leftReceptor.setSensitivity(1.0);
        leftReceptor.setColorFilter("white");
        leftReceptor.setThreshold(10.0);
        receptors.add(leftReceptor);

        ReceptorDefinition rightReceptor = new ReceptorDefinition();
        rightReceptor.setId("sensor_right");
        rightReceptor.setAngleFrom(Math.toRadians(-135));
        rightReceptor.setAngleTo(Math.toRadians(-45));
        rightReceptor.setMaxRange(200.0);
        rightReceptor.setSensitivity(1.0);
        rightReceptor.setColorFilter("white");
        rightReceptor.setThreshold(10.0);
        receptors.add(rightReceptor);

        species.setReceptors(receptors);

        // Neural network: crossed excitatory
        NeuralNetwork network = new NeuralNetwork();

        // Input neurodes
        Neurode inputLeft = createNeurode("input_left", NeurodeType.INPUT, 0);
        Neurode inputRight = createNeurode("input_right", NeurodeType.INPUT, 0);

        // Output neurodes (motors)
        Neurode outputLeft = createNeurode("output_left_motor", NeurodeType.OUTPUT, 1);
        Neurode outputRight = createNeurode("output_right_motor", NeurodeType.OUTPUT, 1);

        network.addNeurode(inputLeft);
        network.addNeurode(inputRight);
        network.addNeurode(outputLeft);
        network.addNeurode(outputRight);

        // Crossed connections: left → right motor, right → left motor
        network.addConnection(createConnection("c1", "input_left", "output_right_motor", ConnectionType.EXCITER, 1.0));
        network.addConnection(createConnection("c2", "input_right", "output_left_motor", ConnectionType.EXCITER, 1.0));

        species.setNeuralNetworkTemplate(network);

        return species;
    }

    /**
     * Creates Photophobe species - flees from light sources.
     * Uses parallel excitatory connections (left sensor → left motor, right sensor → right motor).
     */
    private Species createPhotophobe() {
        Species species = new Species();
        species.setId("photophobe");
        species.setName("Photophobe");
        species.setDescription("Flees from light sources using parallel excitatory connections");

        species.setRadius(10.0);
        species.setWheelBase(15.0);
        species.setMaxSpeed(50.0);
        species.setColorName("red");

        // Same receptors as Phototrope
        List<ReceptorDefinition> receptors = new ArrayList<>();

        ReceptorDefinition leftReceptor = new ReceptorDefinition();
        leftReceptor.setId("sensor_left");
        leftReceptor.setAngleFrom(Math.toRadians(45));
        leftReceptor.setAngleTo(Math.toRadians(135));
        leftReceptor.setMaxRange(200.0);
        leftReceptor.setSensitivity(1.0);
        leftReceptor.setColorFilter("white");
        leftReceptor.setThreshold(10.0);
        receptors.add(leftReceptor);

        ReceptorDefinition rightReceptor = new ReceptorDefinition();
        rightReceptor.setId("sensor_right");
        rightReceptor.setAngleFrom(Math.toRadians(-135));
        rightReceptor.setAngleTo(Math.toRadians(-45));
        rightReceptor.setMaxRange(200.0);
        rightReceptor.setSensitivity(1.0);
        rightReceptor.setColorFilter("white");
        rightReceptor.setThreshold(10.0);
        receptors.add(rightReceptor);

        species.setReceptors(receptors);

        // Neural network: parallel excitatory
        NeuralNetwork network = new NeuralNetwork();

        Neurode inputLeft = createNeurode("input_left", NeurodeType.INPUT, 0);
        Neurode inputRight = createNeurode("input_right", NeurodeType.INPUT, 0);
        Neurode outputLeft = createNeurode("output_left_motor", NeurodeType.OUTPUT, 1);
        Neurode outputRight = createNeurode("output_right_motor", NeurodeType.OUTPUT, 1);

        network.addNeurode(inputLeft);
        network.addNeurode(inputRight);
        network.addNeurode(outputLeft);
        network.addNeurode(outputRight);

        // Parallel connections: left → left motor, right → right motor
        network.addConnection(createConnection("c1", "input_left", "output_left_motor", ConnectionType.EXCITER, 1.0));
        network.addConnection(createConnection("c2", "input_right", "output_right_motor", ConnectionType.EXCITER, 1.0));

        species.setNeuralNetworkTemplate(network);

        return species;
    }

    /**
     * Creates Explorer species - navigates around obstacles.
     * Uses bias with inhibitory connections from obstacle sensors.
     */
    private Species createExplorer() {
        Species species = new Species();
        species.setId("explorer");
        species.setName("Explorer");
        species.setDescription("Explores environment while avoiding obstacles using inhibitory connections");

        species.setRadius(10.0);
        species.setWheelBase(15.0);
        species.setMaxSpeed(40.0);
        species.setColorName("green");

        // 3 receptors: front, left, right
        List<ReceptorDefinition> receptors = new ArrayList<>();

        ReceptorDefinition frontReceptor = new ReceptorDefinition();
        frontReceptor.setId("sensor_front");
        frontReceptor.setAngleFrom(Math.toRadians(-30));
        frontReceptor.setAngleTo(Math.toRadians(30));
        frontReceptor.setMaxRange(100.0);
        frontReceptor.setSensitivity(1.0);
        frontReceptor.setColorFilter("white");
        frontReceptor.setThreshold(8.0);
        receptors.add(frontReceptor);

        ReceptorDefinition leftReceptor = new ReceptorDefinition();
        leftReceptor.setId("sensor_left");
        leftReceptor.setAngleFrom(Math.toRadians(30));
        leftReceptor.setAngleTo(Math.toRadians(90));
        leftReceptor.setMaxRange(100.0);
        leftReceptor.setSensitivity(1.0);
        leftReceptor.setColorFilter("white");
        leftReceptor.setThreshold(8.0);
        receptors.add(leftReceptor);

        ReceptorDefinition rightReceptor = new ReceptorDefinition();
        rightReceptor.setId("sensor_right");
        rightReceptor.setAngleFrom(Math.toRadians(-90));
        rightReceptor.setAngleTo(Math.toRadians(-30));
        rightReceptor.setMaxRange(100.0);
        rightReceptor.setSensitivity(1.0);
        rightReceptor.setColorFilter("white");
        rightReceptor.setThreshold(8.0);
        receptors.add(rightReceptor);

        species.setReceptors(receptors);

        // Neural network: bias + inhibitory
        NeuralNetwork network = new NeuralNetwork();

        // Bias neurode (always fires)
        Neurode bias = createNeurode("bias", NeurodeType.INPUT, 0);
        bias.setWillFireNextTick(true);

        Neurode inputFront = createNeurode("input_front", NeurodeType.INPUT, 0);
        Neurode inputLeft = createNeurode("input_left", NeurodeType.INPUT, 0);
        Neurode inputRight = createNeurode("input_right", NeurodeType.INPUT, 0);
        Neurode outputLeft = createNeurode("output_left_motor", NeurodeType.OUTPUT, 1);
        Neurode outputRight = createNeurode("output_right_motor", NeurodeType.OUTPUT, 1);

        network.addNeurode(bias);
        network.addNeurode(inputFront);
        network.addNeurode(inputLeft);
        network.addNeurode(inputRight);
        network.addNeurode(outputLeft);
        network.addNeurode(outputRight);

        // Bias drives both motors
        network.addConnection(createConnection("c1", "bias", "output_left_motor", ConnectionType.EXCITER, 1.0));
        network.addConnection(createConnection("c2", "bias", "output_right_motor", ConnectionType.EXCITER, 1.0));

        // Inhibitory connections
        network.addConnection(createConnection("c3", "input_front", "output_left_motor", ConnectionType.INHIBITOR, 1.0));
        network.addConnection(createConnection("c4", "input_front", "output_right_motor", ConnectionType.INHIBITOR, 1.0));
        network.addConnection(createConnection("c5", "input_left", "output_left_motor", ConnectionType.INHIBITOR, 1.0));
        network.addConnection(createConnection("c6", "input_right", "output_right_motor", ConnectionType.INHIBITOR, 1.0));

        species.setNeuralNetworkTemplate(network);

        return species;
    }

    /**
     * Creates Aggressive species - charges at targets.
     * Uses bias with crossed inhibitory connections.
     */
    private Species createAggressive() {
        Species species = new Species();
        species.setId("aggressive");
        species.setName("Aggressive");
        species.setDescription("Charges at targets using crossed inhibitory connections");

        species.setRadius(10.0);
        species.setWheelBase(15.0);
        species.setMaxSpeed(60.0);
        species.setColorName("red");

        // 2 narrow front sensors
        List<ReceptorDefinition> receptors = new ArrayList<>();

        ReceptorDefinition leftReceptor = new ReceptorDefinition();
        leftReceptor.setId("sensor_left");
        leftReceptor.setAngleFrom(Math.toRadians(0));
        leftReceptor.setAngleTo(Math.toRadians(45));
        leftReceptor.setMaxRange(150.0);
        leftReceptor.setSensitivity(1.0);
        leftReceptor.setColorFilter("white");
        leftReceptor.setThreshold(10.0);
        receptors.add(leftReceptor);

        ReceptorDefinition rightReceptor = new ReceptorDefinition();
        rightReceptor.setId("sensor_right");
        rightReceptor.setAngleFrom(Math.toRadians(-45));
        rightReceptor.setAngleTo(Math.toRadians(0));
        rightReceptor.setMaxRange(150.0);
        rightReceptor.setSensitivity(1.0);
        rightReceptor.setColorFilter("white");
        rightReceptor.setThreshold(10.0);
        receptors.add(rightReceptor);

        species.setReceptors(receptors);

        // Neural network: bias + crossed inhibitory
        NeuralNetwork network = new NeuralNetwork();

        Neurode bias = createNeurode("bias", NeurodeType.INPUT, 0);
        bias.setWillFireNextTick(true);

        Neurode inputLeft = createNeurode("input_left", NeurodeType.INPUT, 0);
        Neurode inputRight = createNeurode("input_right", NeurodeType.INPUT, 0);
        Neurode outputLeft = createNeurode("output_left_motor", NeurodeType.OUTPUT, 1);
        Neurode outputRight = createNeurode("output_right_motor", NeurodeType.OUTPUT, 1);

        network.addNeurode(bias);
        network.addNeurode(inputLeft);
        network.addNeurode(inputRight);
        network.addNeurode(outputLeft);
        network.addNeurode(outputRight);

        // Bias drives both motors
        network.addConnection(createConnection("c1", "bias", "output_left_motor", ConnectionType.EXCITER, 1.0));
        network.addConnection(createConnection("c2", "bias", "output_right_motor", ConnectionType.EXCITER, 1.0));

        // Crossed inhibitory: left sensor inhibits right motor, right sensor inhibits left motor
        network.addConnection(createConnection("c3", "input_left", "output_right_motor", ConnectionType.INHIBITOR, 1.0));
        network.addConnection(createConnection("c4", "input_right", "output_left_motor", ConnectionType.INHIBITOR, 1.0));

        species.setNeuralNetworkTemplate(network);

        return species;
    }

    /**
     * Creates Coward species - flees from threats approaching from behind.
     * Uses rear sensors with parallel excitatory connections.
     */
    private Species createCoward() {
        Species species = new Species();
        species.setId("coward");
        species.setName("Coward");
        species.setDescription("Flees from threats approaching from behind");

        species.setRadius(10.0);
        species.setWheelBase(15.0);
        species.setMaxSpeed(55.0);
        species.setColorName("green");

        // 2 rear sensors
        List<ReceptorDefinition> receptors = new ArrayList<>();

        ReceptorDefinition rearLeftReceptor = new ReceptorDefinition();
        rearLeftReceptor.setId("sensor_rear_left");
        rearLeftReceptor.setAngleFrom(Math.toRadians(90));
        rearLeftReceptor.setAngleTo(Math.toRadians(180));
        rearLeftReceptor.setMaxRange(150.0);
        rearLeftReceptor.setSensitivity(1.0);
        rearLeftReceptor.setColorFilter("white");
        rearLeftReceptor.setThreshold(10.0);
        receptors.add(rearLeftReceptor);

        ReceptorDefinition rearRightReceptor = new ReceptorDefinition();
        rearRightReceptor.setId("sensor_rear_right");
        rearRightReceptor.setAngleFrom(Math.toRadians(-180));
        rearRightReceptor.setAngleTo(Math.toRadians(-90));
        rearRightReceptor.setMaxRange(150.0);
        rearRightReceptor.setSensitivity(1.0);
        rearRightReceptor.setColorFilter("white");
        rearRightReceptor.setThreshold(10.0);
        receptors.add(rearRightReceptor);

        species.setReceptors(receptors);

        // Neural network: parallel excitatory (same as photophobe but with rear sensors)
        NeuralNetwork network = new NeuralNetwork();

        Neurode inputLeft = createNeurode("input_left", NeurodeType.INPUT, 0);
        Neurode inputRight = createNeurode("input_right", NeurodeType.INPUT, 0);
        Neurode outputLeft = createNeurode("output_left_motor", NeurodeType.OUTPUT, 1);
        Neurode outputRight = createNeurode("output_right_motor", NeurodeType.OUTPUT, 1);

        network.addNeurode(inputLeft);
        network.addNeurode(inputRight);
        network.addNeurode(outputLeft);
        network.addNeurode(outputRight);

        // Parallel connections
        network.addConnection(createConnection("c1", "input_left", "output_left_motor", ConnectionType.EXCITER, 1.0));
        network.addConnection(createConnection("c2", "input_right", "output_right_motor", ConnectionType.EXCITER, 1.0));

        species.setNeuralNetworkTemplate(network);

        return species;
    }

    /**
     * Creates Paranoid species - avoids threats from all directions.
     * Uses 4-direction sensors with complex inhibitory connections.
     */
    private Species createParanoid() {
        Species species = new Species();
        species.setId("paranoid");
        species.setName("Paranoid");
        species.setDescription("Avoids threats from all directions using complex sensor array");

        species.setRadius(10.0);
        species.setWheelBase(15.0);
        species.setMaxSpeed(45.0);
        species.setColorName("blue");

        // 4 sensors: front, rear, left, right
        List<ReceptorDefinition> receptors = new ArrayList<>();

        ReceptorDefinition frontReceptor = new ReceptorDefinition();
        frontReceptor.setId("sensor_front");
        frontReceptor.setAngleFrom(Math.toRadians(-45));
        frontReceptor.setAngleTo(Math.toRadians(45));
        frontReceptor.setMaxRange(120.0);
        frontReceptor.setSensitivity(1.0);
        frontReceptor.setColorFilter("white");
        frontReceptor.setThreshold(8.0);
        receptors.add(frontReceptor);

        ReceptorDefinition rearReceptor = new ReceptorDefinition();
        rearReceptor.setId("sensor_rear");
        rearReceptor.setAngleFrom(Math.toRadians(135));
        rearReceptor.setAngleTo(Math.toRadians(-135));
        rearReceptor.setMaxRange(120.0);
        rearReceptor.setSensitivity(1.0);
        rearReceptor.setColorFilter("white");
        rearReceptor.setThreshold(8.0);
        receptors.add(rearReceptor);

        ReceptorDefinition leftReceptor = new ReceptorDefinition();
        leftReceptor.setId("sensor_left");
        leftReceptor.setAngleFrom(Math.toRadians(45));
        leftReceptor.setAngleTo(Math.toRadians(135));
        leftReceptor.setMaxRange(120.0);
        leftReceptor.setSensitivity(1.0);
        leftReceptor.setColorFilter("white");
        leftReceptor.setThreshold(8.0);
        receptors.add(leftReceptor);

        ReceptorDefinition rightReceptor = new ReceptorDefinition();
        rightReceptor.setId("sensor_right");
        rightReceptor.setAngleFrom(Math.toRadians(-135));
        rightReceptor.setAngleTo(Math.toRadians(-45));
        rightReceptor.setMaxRange(120.0);
        rightReceptor.setSensitivity(1.0);
        rightReceptor.setColorFilter("white");
        rightReceptor.setThreshold(8.0);
        receptors.add(rightReceptor);

        species.setReceptors(receptors);

        // Neural network: bias + complex inhibitory from all directions
        NeuralNetwork network = new NeuralNetwork();

        Neurode bias = createNeurode("bias", NeurodeType.INPUT, 0);
        bias.setWillFireNextTick(true);

        Neurode inputFront = createNeurode("input_front", NeurodeType.INPUT, 0);
        Neurode inputRear = createNeurode("input_rear", NeurodeType.INPUT, 0);
        Neurode inputLeft = createNeurode("input_left", NeurodeType.INPUT, 0);
        Neurode inputRight = createNeurode("input_right", NeurodeType.INPUT, 0);
        Neurode outputLeft = createNeurode("output_left_motor", NeurodeType.OUTPUT, 1);
        Neurode outputRight = createNeurode("output_right_motor", NeurodeType.OUTPUT, 1);

        network.addNeurode(bias);
        network.addNeurode(inputFront);
        network.addNeurode(inputRear);
        network.addNeurode(inputLeft);
        network.addNeurode(inputRight);
        network.addNeurode(outputLeft);
        network.addNeurode(outputRight);

        // Bias drives both motors
        network.addConnection(createConnection("c1", "bias", "output_left_motor", ConnectionType.EXCITER, 1.0));
        network.addConnection(createConnection("c2", "bias", "output_right_motor", ConnectionType.EXCITER, 1.0));

        // Inhibitory from all directions to both motors
        network.addConnection(createConnection("c3", "input_front", "output_left_motor", ConnectionType.INHIBITOR, 1.0));
        network.addConnection(createConnection("c4", "input_front", "output_right_motor", ConnectionType.INHIBITOR, 1.0));
        network.addConnection(createConnection("c5", "input_rear", "output_left_motor", ConnectionType.INHIBITOR, 1.0));
        network.addConnection(createConnection("c6", "input_rear", "output_right_motor", ConnectionType.INHIBITOR, 1.0));
        network.addConnection(createConnection("c7", "input_left", "output_left_motor", ConnectionType.INHIBITOR, 1.0));
        network.addConnection(createConnection("c8", "input_right", "output_right_motor", ConnectionType.INHIBITOR, 1.0));

        species.setNeuralNetworkTemplate(network);

        return species;
    }

    /**
     * Helper: Creates a neurode.
     */
    private Neurode createNeurode(String id, NeurodeType type, int threshold) {
        Neurode neurode = new Neurode();
        neurode.setId(id);
        neurode.setType(type);
        neurode.setThreshold(threshold);
        return neurode;
    }

    /**
     * Helper: Creates a connection.
     */
    private Connection createConnection(String id, String from, String to, ConnectionType type, double weight) {
        Connection conn = new Connection();
        conn.setId(id);
        conn.setFromNeurodeId(from);
        conn.setToNeurodeId(to);
        conn.setType(type);
        conn.setWeight(weight);
        return conn;
    }

    /**
     * Writes package to JSON file.
     */
    private void writePackage(SpeciesPackage pkg, String outputPath) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        // Ensure directory exists
        java.io.File file = new java.io.File(outputPath);
        file.getParentFile().mkdirs();

        try (FileWriter writer = new FileWriter(outputPath)) {
            gson.toJson(pkg, writer);
        }
    }

    /**
     * Species package container.
     */
    public static class SpeciesPackage {
        private String id;
        private String name;
        private String version;
        private String description;
        private List<Species> species;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public List<Species> getSpecies() { return species; }
        public void setSpecies(List<Species> species) { this.species = species; }
    }
}
