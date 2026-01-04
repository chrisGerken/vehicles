package com.gerkenip.vehicles.model;

/**
 * Type of neurode in neural network.
 */
public enum NeurodeType {
    /** Input neurode connected to receptor or constant */
    INPUT,

    /** Hidden neurode for internal processing */
    HIDDEN,

    /** Output neurode that controls motor */
    OUTPUT
}
