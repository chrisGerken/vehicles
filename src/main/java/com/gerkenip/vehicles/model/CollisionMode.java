package com.gerkenip.vehicles.model;

/**
 * Collision behavior for vehicles.
 */
public enum CollisionMode {
    /** No collision detection, vehicles pass through objects */
    NONE,

    /** Vehicles break (stop/destroy) when colliding */
    BREAK,

    /** Vehicles bounce off objects */
    BOUNCE
}
