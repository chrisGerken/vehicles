package com.gerkenip.vehicles.model;

/**
 * Type of connection between neurodes.
 */
public enum ConnectionType {
    /** If source fires, prevents target from firing */
    INHIBITOR,

    /** If source fires, contributes to target activation */
    EXCITER
}
