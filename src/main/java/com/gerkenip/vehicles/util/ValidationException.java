package com.gerkenip.vehicles.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Exception thrown when validation fails.
 * HTTP 400 Bad Request.
 */
public class ValidationException extends SimulationException {
    private final List<ValidationError> details;

    public ValidationException(String message) {
        super(message, 400, "VALIDATION_ERROR");
        this.details = new ArrayList<>();
    }

    public ValidationException(String message, List<ValidationError> details) {
        super(message, 400, "VALIDATION_ERROR");
        this.details = details != null ? details : new ArrayList<>();
    }

    public List<ValidationError> getDetails() {
        return details;
    }

    public void addDetail(String field, Object value, String error) {
        details.add(new ValidationError(field, value, error));
    }

    /**
     * Validation error detail.
     */
    public static class ValidationError {
        private final String field;
        private final Object value;
        private final String error;

        public ValidationError(String field, Object value, String error) {
            this.field = field;
            this.value = value;
            this.error = error;
        }

        public String getField() {
            return field;
        }

        public Object getValue() {
            return value;
        }

        public String getError() {
            return error;
        }
    }
}
