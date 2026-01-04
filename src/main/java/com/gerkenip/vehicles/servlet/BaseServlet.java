package com.gerkenip.vehicles.servlet;

import com.gerkenip.vehicles.util.*;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Base servlet with common functionality for all servlets.
 */
public abstract class BaseServlet extends HttpServlet {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * Reads JSON request body.
     */
    protected String readRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    /**
     * Sends JSON response.
     */
    protected void sendJsonResponse(HttpServletResponse response, int status, Object data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String json = JsonUtil.toJson(data);

        try (PrintWriter out = response.getWriter()) {
            out.print(json);
            out.flush();
        }
    }

    /**
     * Sends error response in standard format.
     */
    protected void sendErrorResponse(HttpServletResponse response, int status, String errorCode,
                                     String message, Object details) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", true);
        error.put("errorCode", errorCode);
        error.put("message", message);
        if (details != null) {
            error.put("details", details);
        }
        error.put("timestamp", System.currentTimeMillis());

        sendJsonResponse(response, status, error);
    }

    /**
     * Extracts path parameter from URL.
     * Example: /api/simulation/sim_123 with index=3 returns "sim_123"
     */
    protected String extractPathParam(HttpServletRequest request, int index) {
        String pathInfo = request.getPathInfo();
        if (pathInfo == null || pathInfo.isEmpty()) {
            return null;
        }

        String[] parts = pathInfo.split("/");
        if (index >= 0 && index < parts.length) {
            return parts[index];
        }

        return null;
    }

    /**
     * Gets query parameter.
     */
    protected String getQueryParam(HttpServletRequest request, String name) {
        return request.getParameter(name);
    }

    /**
     * Handles exceptions and sends appropriate error responses.
     */
    protected void handleException(HttpServletResponse response, Exception e) throws IOException {
        if (e instanceof ValidationException) {
            ValidationException ve = (ValidationException) e;
            sendErrorResponse(response, ve.getHttpStatusCode(), ve.getErrorCode(),
                            ve.getMessage(), ve.getDetails());
        } else if (e instanceof ResourceNotFoundException) {
            ResourceNotFoundException nfe = (ResourceNotFoundException) e;
            sendErrorResponse(response, nfe.getHttpStatusCode(), nfe.getErrorCode(),
                            nfe.getMessage(), null);
        } else if (e instanceof SimulationStateException) {
            SimulationStateException sse = (SimulationStateException) e;
            sendErrorResponse(response, sse.getHttpStatusCode(), sse.getErrorCode(),
                            sse.getMessage(), null);
        } else if (e instanceof JsonSyntaxException) {
            sendErrorResponse(response, 400, "INVALID_JSON", "Malformed JSON: " + e.getMessage(), null);
        } else {
            logger.error("Unexpected error", e);
            sendErrorResponse(response, 500, "INTERNAL_ERROR",
                            "An unexpected error occurred: " + e.getMessage(), null);
        }
    }
}
