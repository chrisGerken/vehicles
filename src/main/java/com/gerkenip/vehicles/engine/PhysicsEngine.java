package com.gerkenip.vehicles.engine;

import com.gerkenip.vehicles.model.*;
import com.gerkenip.vehicles.util.MathUtil;

/**
 * Physics engine implementing differential drive kinematics and collision detection.
 *
 * CRITICAL COMPONENT: Implements exact differential drive formulas from design.md.
 */
public class PhysicsEngine {

    /**
     * Updates vehicle position using differential drive kinematics.
     *
     * Formulas from design.md:
     * linearVelocity = (vL + vR) / 2
     * angularVelocity = (vR - vL) / wheelBase
     * angle += angularVelocity × dt
     * x += linearVelocity × cos(angle) × dt
     * y += linearVelocity × sin(angle) × dt
     *
     * @param vehicle The vehicle to update
     * @param deltaTime Time step in simulation units
     */
    public void updateVehiclePosition(Vehicle vehicle, double deltaTime) {
        // Get motor speeds and convert to wheel velocities
        double vL = vehicle.getLeftMotorSpeed() * vehicle.getMaxSpeed();
        double vR = vehicle.getRightMotorSpeed() * vehicle.getMaxSpeed();

        // Calculate linear and angular velocities
        double linearVelocity = (vL + vR) / 2.0;
        double angularVelocity = (vR - vL) / vehicle.getWheelBase();

        // Update angle
        double newAngle = vehicle.getAngle() + (angularVelocity * deltaTime);
        vehicle.setAngle(MathUtil.normalizeAngle(newAngle));

        // Update position
        double newX = vehicle.getX() + (linearVelocity * Math.cos(newAngle) * deltaTime);
        double newY = vehicle.getY() + (linearVelocity * Math.sin(newAngle) * deltaTime);

        vehicle.setX(newX);
        vehicle.setY(newY);
    }

    /**
     * Detects collision between vehicle and arena boundaries or objects.
     *
     * @param vehicle The vehicle to check
     * @param arena The arena
     * @param collisionBehavior Collision configuration
     * @return true if collision detected, false otherwise
     */
    public boolean detectCollision(Vehicle vehicle, Arena arena, CollisionBehavior collisionBehavior) {
        // Check boundary collision
        if (arena.checkBoundaryCollision(vehicle)) {
            return true;
        }

        // Get collision mode for this vehicle's color
        CollisionMode mode = collisionBehavior.getBehaviorForColor(vehicle.getColorName());

        // If mode is NONE, skip collision detection
        if (mode == CollisionMode.NONE) {
            return false;
        }

        // Check collision with static objects
        for (StaticSimulationObject obj : arena.getStaticObjects()) {
            if (detectObjectCollision(vehicle, obj)) {
                return true;
            }
        }

        // Check collision with other vehicles (simplified - just check radius overlap)
        for (Vehicle other : arena.getVehicles()) {
            if (other != vehicle) {
                double distance = vehicle.distanceTo(other);
                if (distance < vehicle.getRadius() + other.getRadius()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Detects collision between vehicle and a static object.
     *
     * @param vehicle The vehicle
     * @param obj The static object
     * @return true if collision detected
     */
    private boolean detectObjectCollision(Vehicle vehicle, StaticSimulationObject obj) {
        if (obj.getType() == ObjectType.POINT) {
            // Circle-circle collision
            double distance = vehicle.distanceTo(obj);
            return distance < vehicle.getRadius() + obj.getRadius();
        } else if (obj.getType() == ObjectType.WALL) {
            // Circle-line collision
            double distance = MathUtil.pointToLineDistance(
                    vehicle.getX(), vehicle.getY(),
                    obj.getX(), obj.getY(),
                    obj.getX2(), obj.getY2()
            );
            return distance < vehicle.getRadius();
        }

        return false;
    }

    /**
     * Handles collision based on collision mode.
     *
     * @param vehicle The vehicle that collided
     * @param collisionBehavior Collision configuration
     * @return true if vehicle should be removed (BREAK mode)
     */
    public boolean handleCollision(Vehicle vehicle, CollisionBehavior collisionBehavior) {
        CollisionMode mode = collisionBehavior.getBehaviorForColor(vehicle.getColorName());

        switch (mode) {
            case BREAK:
                // Vehicle breaks - should be removed
                return true;

            case BOUNCE:
                // Vehicle bounces - reverse direction
                handleBounce(vehicle);
                return false;

            case NONE:
            default:
                // No collision handling
                return false;
        }
    }

    /**
     * Handles bounce collision: reverses vehicle direction.
     *
     * @param vehicle The vehicle to bounce
     */
    public void handleBounce(Vehicle vehicle) {
        // Reverse direction by adding π to angle
        double newAngle = vehicle.getAngle() + Math.PI;
        vehicle.setAngle(MathUtil.normalizeAngle(newAngle));

        // Optionally reverse motor speeds to back away
        double temp = vehicle.getLeftMotorSpeed();
        vehicle.setLeftMotorSpeed(-vehicle.getRightMotorSpeed());
        vehicle.setRightMotorSpeed(-temp);
    }

    /**
     * Applies arena wrapping to vehicle position if wrapping is enabled.
     *
     * @param vehicle The vehicle
     * @param arena The arena
     */
    public void applyWrapping(Vehicle vehicle, Arena arena) {
        double[] normalized = arena.normalizePosition(vehicle.getX(), vehicle.getY());
        vehicle.setX(normalized[0]);
        vehicle.setY(normalized[1]);
    }
}
