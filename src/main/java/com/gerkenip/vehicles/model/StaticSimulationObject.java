package com.gerkenip.vehicles.model;

/**
 * Static (non-moving) simulation object.
 * Can be a point light source or a wall obstacle.
 */
public class StaticSimulationObject extends SimulationObject {
    private ObjectType type;  // POINT or WALL
    private Double x2;        // End X coordinate (for WALL type only, nullable)
    private Double y2;        // End Y coordinate (for WALL type only, nullable)
    private boolean emitsBrightness; // Whether it's a light source

    public StaticSimulationObject() {
        super();
    }

    /**
     * Constructor for POINT type.
     */
    public StaticSimulationObject(String id, double x, double y, String colorName,
                                  double brightness, double radius, boolean emitsBrightness) {
        super(id, x, y, colorName, brightness, radius);
        this.type = ObjectType.POINT;
        this.emitsBrightness = emitsBrightness;
    }

    /**
     * Constructor for WALL type.
     */
    public StaticSimulationObject(String id, double x, double y, double x2, double y2,
                                  String colorName, double brightness, boolean emitsBrightness) {
        super(id, x, y, colorName, brightness, 0);  // Walls have no radius
        this.type = ObjectType.WALL;
        this.x2 = x2;
        this.y2 = y2;
        this.emitsBrightness = emitsBrightness;
    }

    // Getters and setters
    public ObjectType getType() {
        return type;
    }

    public void setType(ObjectType type) {
        this.type = type;
    }

    public Double getX2() {
        return x2;
    }

    public void setX2(Double x2) {
        this.x2 = x2;
    }

    public Double getY2() {
        return y2;
    }

    public void setY2(Double y2) {
        this.y2 = y2;
    }

    public boolean isEmitsBrightness() {
        return emitsBrightness;
    }

    public void setEmitsBrightness(boolean emitsBrightness) {
        this.emitsBrightness = emitsBrightness;
    }

    @Override
    public String toString() {
        if (type == ObjectType.POINT) {
            return "StaticObject{" + id + " POINT at (" + x + "," + y + ") r=" + radius +
                   " color=" + colorName + " brightness=" + brightness + "}";
        } else {
            return "StaticObject{" + id + " WALL from (" + x + "," + y + ") to (" + x2 + "," + y2 +
                   ") color=" + colorName + "}";
        }
    }
}
