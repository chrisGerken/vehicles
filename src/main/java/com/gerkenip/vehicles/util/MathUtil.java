package com.gerkenip.vehicles.util;

/**
 * Mathematical utility functions for physics and geometry.
 * CRITICAL COMPONENT: Used throughout the simulation for accurate calculations.
 */
public class MathUtil {

    /**
     * Normalizes angle to [0, 2π] range.
     */
    public static double normalizeAngle(double angle) {
        while (angle < 0) {
            angle += 2 * Math.PI;
        }
        while (angle >= 2 * Math.PI) {
            angle -= 2 * Math.PI;
        }
        return angle;
    }

    /**
     * Normalizes angle to [-π, π] range.
     */
    public static double normalizeAngleSigned(double angle) {
        while (angle > Math.PI) {
            angle -= 2 * Math.PI;
        }
        while (angle <= -Math.PI) {
            angle += 2 * Math.PI;
        }
        return angle;
    }

    /**
     * Calculates shortest angular distance between two angles.
     * Returns value in [-π, π].
     */
    public static double angleDifference(double angle1, double angle2) {
        double diff = angle2 - angle1;
        return normalizeAngleSigned(diff);
    }

    /**
     * Checks if an angle is between fromAngle and toAngle (handles wraparound).
     * All angles should be in radians.
     *
     * @param angle The angle to check
     * @param fromAngle Start of range
     * @param toAngle End of range
     * @return true if angle is within the range
     */
    public static boolean isAngleBetween(double angle, double fromAngle, double toAngle) {
        // Normalize all angles to [-π, π]
        angle = normalizeAngleSigned(angle);
        fromAngle = normalizeAngleSigned(fromAngle);
        toAngle = normalizeAngleSigned(toAngle);

        if (fromAngle <= toAngle) {
            // No wraparound
            return angle >= fromAngle && angle <= toAngle;
        } else {
            // Wraparound case (e.g., from 150° to -150°)
            return angle >= fromAngle || angle <= toAngle;
        }
    }

    /**
     * Calculates distance from point (px, py) to line segment from (x1, y1) to (x2, y2).
     * Used for wall collision detection.
     *
     * @param px Point X
     * @param py Point Y
     * @param x1 Line start X
     * @param y1 Line start Y
     * @param x2 Line end X
     * @param y2 Line end Y
     * @return Distance from point to line segment
     */
    public static double pointToLineDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;

        // If line segment is actually a point
        if (dx == 0 && dy == 0) {
            return Math.sqrt((px - x1) * (px - x1) + (py - y1) * (py - y1));
        }

        // Calculate parameter t that represents position along line segment
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);

        // Clamp t to [0, 1] to stay on segment
        t = Math.max(0, Math.min(1, t));

        // Find closest point on line segment
        double closestX = x1 + t * dx;
        double closestY = y1 + t * dy;

        // Calculate distance from point to closest point
        return Math.sqrt((px - closestX) * (px - closestX) + (py - closestY) * (py - closestY));
    }

    /**
     * Clamps a value between min and max.
     */
    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Clamps a value between -1.0 and 1.0 (for motor speeds).
     */
    public static double clampMotorSpeed(double speed) {
        return clamp(speed, -1.0, 1.0);
    }

    /**
     * Calculates distance between two points.
     */
    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Converts degrees to radians.
     */
    public static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180.0;
    }

    /**
     * Converts radians to degrees.
     */
    public static double radiansToDegrees(double radians) {
        return radians * 180.0 / Math.PI;
    }
}
