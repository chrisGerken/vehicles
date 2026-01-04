package com.gerkenip.vehicles.util;

/**
 * Base exception for all simulation-related errors.
 */
public class SimulationException extends RuntimeException {
    private final int httpStatusCode;
    private final String errorCode;

    public SimulationException(String message, int httpStatusCode, String errorCode) {
        super(message);
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorCode;
    }

    public SimulationException(String message, Throwable cause, int httpStatusCode, String errorCode) {
        super(message, cause);
        this.httpStatusCode = httpStatusCode;
        this.errorCode = errorCode;
    }

    public int getHttpStatusCode() {
        return httpStatusCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
