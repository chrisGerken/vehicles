package com.gerkenip.vehicles.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Species definition that serves as a template for creating vehicles.
 */
public class Species {
    private String id;
    private String name;
    private String colorName;  // Default color for vehicles (optional)

    // Template components
    private List<ReceptorDefinition> receptorDefinitions;
    private NeuralNetwork neuralNetworkTemplate;

    // Physics parameters
    private double vehicleRadius;
    private double wheelBase;
    private double maxSpeed;

    // Package support
    private String sourcePackage;  // Reference to reusable package (optional)
    private boolean editable;      // Whether this species can be modified

    public Species() {
        this.receptorDefinitions = new ArrayList<>();
        this.editable = true;
    }

    public Species(String id, String name, String colorName, List<ReceptorDefinition> receptorDefinitions,
                  NeuralNetwork neuralNetworkTemplate, double vehicleRadius, double wheelBase, double maxSpeed) {
        this.id = id;
        this.name = name;
        this.colorName = colorName;
        this.receptorDefinitions = receptorDefinitions != null ? receptorDefinitions : new ArrayList<>();
        this.neuralNetworkTemplate = neuralNetworkTemplate;
        this.vehicleRadius = vehicleRadius;
        this.wheelBase = wheelBase;
        this.maxSpeed = maxSpeed;
        this.editable = true;
    }

    /**
     * Creates a vehicle instance from this species.
     */
    public Vehicle createVehicle(String vehicleId, double x, double y, double angle) {
        return createVehicle(vehicleId, x, y, angle, null);
    }

    /**
     * Creates a vehicle instance with specific color.
     */
    public Vehicle createVehicle(String vehicleId, double x, double y, double angle, String vehicleColorName) {
        // Determine effective color
        String effectiveColor = vehicleColorName != null ? vehicleColorName : this.colorName;

        // Create receptors from definitions
        List<Receptor> receptors = new ArrayList<>();
        for (ReceptorDefinition def : receptorDefinitions) {
            receptors.add(def.createReceptor());
        }

        // Clone neural network (deep copy needed)
        NeuralNetwork networkInstance = cloneNeuralNetwork(neuralNetworkTemplate);

        // Create vehicle
        Vehicle vehicle = new Vehicle(vehicleId, x, y, effectiveColor, 0.8, vehicleRadius);
        vehicle.setAngle(angle);
        vehicle.setWheelBase(wheelBase);
        vehicle.setMaxSpeed(maxSpeed);
        vehicle.setSpecies(this);
        vehicle.setReceptors(receptors);
        vehicle.setNeuralNetwork(networkInstance);

        return vehicle;
    }

    /**
     * Creates a deep copy of the neural network template.
     */
    private NeuralNetwork cloneNeuralNetwork(NeuralNetwork template) {
        NeuralNetwork clone = new NeuralNetwork();

        // Clone neurodes
        for (Neurode neurode : template.getNeurodes().values()) {
            Neurode neurodeClone = new Neurode(neurode.getId(), neurode.getType(), neurode.getThreshold());
            clone.addNeurode(neurodeClone);
        }

        // Clone connections
        for (Connection conn : template.getConnections()) {
            Connection connClone = new Connection(conn.getId(), conn.getFromNeurodeId(),
                                                 conn.getToNeurodeId(), conn.getType(), conn.getWeight());
            clone.addConnection(connClone);
        }

        return clone;
    }

    /**
     * Creates a clone of this species for mutations/variations.
     */
    public Species clone() {
        Species clone = new Species(this.id + "_clone", this.name + " (clone)", this.colorName,
                                   new ArrayList<>(this.receptorDefinitions),
                                   cloneNeuralNetwork(this.neuralNetworkTemplate),
                                   this.vehicleRadius, this.wheelBase, this.maxSpeed);
        clone.setEditable(true);
        return clone;
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

    public String getColorName() {
        return colorName;
    }

    public void setColorName(String colorName) {
        this.colorName = colorName;
    }

    public List<ReceptorDefinition> getReceptorDefinitions() {
        return receptorDefinitions;
    }

    public void setReceptorDefinitions(List<ReceptorDefinition> receptorDefinitions) {
        this.receptorDefinitions = receptorDefinitions;
    }

    public NeuralNetwork getNeuralNetworkTemplate() {
        return neuralNetworkTemplate;
    }

    public void setNeuralNetworkTemplate(NeuralNetwork neuralNetworkTemplate) {
        this.neuralNetworkTemplate = neuralNetworkTemplate;
    }

    public double getVehicleRadius() {
        return vehicleRadius;
    }

    public void setVehicleRadius(double vehicleRadius) {
        this.vehicleRadius = vehicleRadius;
    }

    public double getWheelBase() {
        return wheelBase;
    }

    public void setWheelBase(double wheelBase) {
        this.wheelBase = wheelBase;
    }

    public double getMaxSpeed() {
        return maxSpeed;
    }

    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }

    public String getSourcePackage() {
        return sourcePackage;
    }

    public void setSourcePackage(String sourcePackage) {
        this.sourcePackage = sourcePackage;
    }

    public boolean isEditable() {
        return editable;
    }

    public void setEditable(boolean editable) {
        this.editable = editable;
    }

    @Override
    public String toString() {
        return "Species{" + name + " [" + id + "], radius=" + vehicleRadius +
               ", wheelBase=" + wheelBase + ", maxSpeed=" + maxSpeed + "}";
    }
}
