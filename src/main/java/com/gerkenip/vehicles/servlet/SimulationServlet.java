package com.gerkenip.vehicles.servlet;

import com.gerkenip.vehicles.model.*;
import com.gerkenip.vehicles.service.SimulationService;
import com.gerkenip.vehicles.util.JsonUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet for simulation control endpoints.
 *
 * Endpoints:
 * - POST /api/simulation - Create simulation
 * - GET /api/simulation - List simulations
 * - GET /api/simulation/{id} - Get simulation
 * - POST /api/simulation/{id}/start - Start
 * - POST /api/simulation/{id}/stop - Stop
 * - POST /api/simulation/{id}/step - Step
 * - POST /api/simulation/{id}/reset - Reset
 * - DELETE /api/simulation/{id} - Delete
 * - GET /api/simulation/{id}/status - Get status
 * - POST /api/simulation/{id}/vehicles - Add vehicle
 * - POST /api/simulation/{id}/species - Add species
 * - POST /api/simulation/{id}/objects - Add static object
 */
@WebServlet("/api/simulation/*")
public class SimulationServlet extends BaseServlet {

    private final SimulationService service = SimulationService.getInstance();

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String pathInfo = request.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                // POST /api/simulation - Create simulation
                handleCreateSimulation(request, response);
            } else {
                String[] parts = pathInfo.split("/");
                if (parts.length >= 2) {
                    String simulationId = parts[1];

                    if (parts.length == 2) {
                        // No action specified - invalid
                        sendErrorResponse(response, 400, "INVALID_REQUEST",
                                        "Action required (start/stop/step/reset)", null);
                    } else {
                        String action = parts[2];

                        switch (action) {
                            case "start":
                                handleStart(simulationId, response);
                                break;
                            case "stop":
                                handleStop(simulationId, response);
                                break;
                            case "step":
                                handleStep(simulationId, response);
                                break;
                            case "reset":
                                handleReset(simulationId, response);
                                break;
                            case "vehicles":
                                handleAddVehicle(simulationId, request, response);
                                break;
                            case "species":
                                handleAddSpecies(simulationId, request, response);
                                break;
                            case "objects":
                                handleAddObject(simulationId, request, response);
                                break;
                            default:
                                sendErrorResponse(response, 400, "INVALID_ACTION",
                                                "Unknown action: " + action, null);
                        }
                    }
                }
            }
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String pathInfo = request.getPathInfo();

            if (pathInfo == null || pathInfo.equals("/")) {
                // GET /api/simulation - List all
                handleListSimulations(response);
            } else {
                String[] parts = pathInfo.split("/");
                if (parts.length >= 2) {
                    String simulationId = parts[1];

                    if (parts.length == 2) {
                        // GET /api/simulation/{id} - Get simulation
                        handleGetSimulation(simulationId, response);
                    } else if (parts.length == 3 && "status".equals(parts[2])) {
                        // GET /api/simulation/{id}/status - Get status
                        handleGetStatus(simulationId, response);
                    }
                }
            }
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            String pathInfo = request.getPathInfo();
            if (pathInfo != null) {
                String[] parts = pathInfo.split("/");
                if (parts.length >= 2) {
                    String simulationId = parts[1];
                    handleDeleteSimulation(simulationId, response);
                }
            }
        } catch (Exception e) {
            handleException(response, e);
        }
    }

    // Handler methods

    private void handleCreateSimulation(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String json = readRequestBody(request);
        Map<String, Object> data = JsonUtil.fromJson(json, Map.class);

        String name = (String) data.get("name");
        Map<String, Object> arenaData = (Map<String, Object>) data.get("arena");

        // Create arena
        Arena arena = new Arena();
        arena.setWidth(((Number) arenaData.get("width")).doubleValue());
        arena.setHeight(((Number) arenaData.get("height")).doubleValue());
        arena.setWrapEastWest((Boolean) arenaData.getOrDefault("wrapEastWest", false));
        arena.setWrapNorthSouth((Boolean) arenaData.getOrDefault("wrapNorthSouth", false));

        Simulation simulation = service.createSimulation(name != null ? name : "Simulation", arena);

        Map<String, Object> result = new HashMap<>();
        result.put("id", simulation.getId());
        result.put("name", simulation.getName());

        sendJsonResponse(response, 201, result);
    }

    private void handleListSimulations(HttpServletResponse response) throws IOException {
        Map<String, Simulation> simulations = service.listSimulations();

        // Return lightweight list
        Map<String, Map<String, Object>> result = new HashMap<>();
        for (Map.Entry<String, Simulation> entry : simulations.entrySet()) {
            Simulation sim = entry.getValue();
            Map<String, Object> simData = new HashMap<>();
            simData.put("id", sim.getId());
            simData.put("name", sim.getName());
            simData.put("tick", sim.getCurrentTick());
            simData.put("running", sim.isRunning());
            result.put(entry.getKey(), simData);
        }

        sendJsonResponse(response, 200, result);
    }

    private void handleGetSimulation(String id, HttpServletResponse response) throws IOException {
        Simulation simulation = service.getSimulation(id);
        sendJsonResponse(response, 200, simulation);
    }

    private void handleStart(String id, HttpServletResponse response) throws IOException {
        service.startSimulation(id);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "started");
        result.put("tick", service.getSimulation(id).getCurrentTick());

        sendJsonResponse(response, 200, result);
    }

    private void handleStop(String id, HttpServletResponse response) throws IOException {
        service.stopSimulation(id);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "stopped");
        result.put("tick", service.getSimulation(id).getCurrentTick());

        sendJsonResponse(response, 200, result);
    }

    private void handleStep(String id, HttpServletResponse response) throws IOException {
        service.stepSimulation(id);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "stepped");
        result.put("tick", service.getSimulation(id).getCurrentTick());

        sendJsonResponse(response, 200, result);
    }

    private void handleReset(String id, HttpServletResponse response) throws IOException {
        service.resetSimulation(id);

        Map<String, Object> result = new HashMap<>();
        result.put("status", "reset");
        result.put("tick", 0);

        sendJsonResponse(response, 200, result);
    }

    private void handleDeleteSimulation(String id, HttpServletResponse response) throws IOException {
        service.deleteSimulation(id);

        Map<String, Object> result = new HashMap<>();
        result.put("deleted", true);

        sendJsonResponse(response, 200, result);
    }

    private void handleGetStatus(String id, HttpServletResponse response) throws IOException {
        Map<String, Object> status = service.getStatus(id);
        sendJsonResponse(response, 200, status);
    }

    private void handleAddVehicle(String simulationId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String json = readRequestBody(request);
        Map<String, Object> data = JsonUtil.fromJson(json, Map.class);

        String speciesId = (String) data.get("speciesId");
        double x = ((Number) data.get("x")).doubleValue();
        double y = ((Number) data.get("y")).doubleValue();
        double angle = ((Number) data.get("angle")).doubleValue();

        Vehicle vehicle = service.addVehicle(simulationId, speciesId, x, y, angle);

        Map<String, Object> result = new HashMap<>();
        result.put("id", vehicle.getId());
        result.put("speciesId", speciesId);

        sendJsonResponse(response, 201, result);
    }

    private void handleAddSpecies(String simulationId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // This would require complex JSON parsing - simplified for MVP
        Map<String, Object> result = new HashMap<>();
        result.put("message", "Species creation via API not yet implemented - use code to create species");
        sendJsonResponse(response, 501, result);
    }

    private void handleAddObject(String simulationId, HttpServletRequest request, HttpServletResponse response) throws IOException {
        String json = readRequestBody(request);
        Map<String, Object> data = JsonUtil.fromJson(json, Map.class);

        String type = (String) data.get("type");
        StaticSimulationObject object;

        if ("POINT".equals(type)) {
            double x = ((Number) data.get("x")).doubleValue();
            double y = ((Number) data.get("y")).doubleValue();
            double radius = ((Number) data.getOrDefault("radius", 10)).doubleValue();
            String colorName = (String) data.getOrDefault("colorName", "white");
            double brightness = ((Number) data.getOrDefault("brightness", 1.0)).doubleValue();
            boolean emits = (Boolean) data.getOrDefault("emitsBrightness", true);

            object = new StaticSimulationObject(null, x, y, colorName, brightness, radius, emits);
        } else if ("WALL".equals(type)) {
            double x = ((Number) data.get("x")).doubleValue();
            double y = ((Number) data.get("y")).doubleValue();
            double x2 = ((Number) data.get("x2")).doubleValue();
            double y2 = ((Number) data.get("y2")).doubleValue();
            String colorName = (String) data.getOrDefault("colorName", "gray");

            object = new StaticSimulationObject(null, x, y, x2, y2, colorName, 0.0, false);
        } else {
            sendErrorResponse(response, 400, "INVALID_TYPE", "Type must be POINT or WALL", null);
            return;
        }

        object = service.addStaticObject(simulationId, object);

        Map<String, Object> result = new HashMap<>();
        result.put("id", object.getId());
        result.put("type", type);

        sendJsonResponse(response, 201, result);
    }
}
