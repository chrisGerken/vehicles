package com.gerkenip.vehicles.util;

/**
 * Exception thrown when a resource is not found.
 * HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends SimulationException {
    public ResourceNotFoundException(String message) {
        super(message, 404, "NOT_FOUND");
    }

    public ResourceNotFoundException(String resourceType, String id) {
        super(resourceType + " not found: " + id, 404, "NOT_FOUND");
    }
}
