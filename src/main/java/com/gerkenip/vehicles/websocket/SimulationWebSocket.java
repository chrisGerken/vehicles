package com.gerkenip.vehicles.websocket;

import com.gerkenip.vehicles.engine.SimulationEngine;
import com.gerkenip.vehicles.engine.SimulationState;
import com.gerkenip.vehicles.engine.StateListener;
import com.gerkenip.vehicles.service.SimulationService;
import com.gerkenip.vehicles.util.JsonUtil;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket endpoint for real-time simulation state updates.
 *
 * Clients send:
 * {"type": "subscribe", "simulationId": "sim_123"}
 *
 * Server sends:
 * {
 *   "type": "state_update",
 *   "simulationId": "sim_123",
 *   "tick": 12345,
 *   "vehicles": [...],
 *   "staticObjects": [...]
 * }
 */
@WebSocket
public class SimulationWebSocket implements StateListener {
    private static final Logger logger = LoggerFactory.getLogger(SimulationWebSocket.class);

    // Track sessions and their subscriptions
    private static final Map<Session, String> sessionSubscriptions = new ConcurrentHashMap<>();

    private Session session;
    private String subscribedSimulationId;

    @OnWebSocketConnect
    public void onConnect(Session session) {
        this.session = session;
        logger.info("WebSocket client connected: {}", session.getRemoteAddress());
    }

    @OnWebSocketMessage
    public void onMessage(Session session, String message) {
        try {
            // Parse message
            Map<String, Object> data = JsonUtil.fromJson(message, Map.class);
            String type = (String) data.get("type");

            if ("subscribe".equals(type)) {
                String simulationId = (String) data.get("simulationId");
                handleSubscribe(simulationId);
            } else {
                logger.warn("Unknown message type: {}", type);
            }
        } catch (Exception e) {
            logger.error("Error handling WebSocket message", e);
        }
    }

    @OnWebSocketClose
    public void onClose(Session session, int statusCode, String reason) {
        logger.info("WebSocket client disconnected: {} (code: {}, reason: {})",
                   session.getRemoteAddress(), statusCode, reason);

        // Unsubscribe
        if (subscribedSimulationId != null) {
            try {
                SimulationEngine engine = SimulationService.getInstance().getEngine(subscribedSimulationId);
                engine.removeStateListener(this);
            } catch (Exception e) {
                // Engine may have been removed
            }
        }

        sessionSubscriptions.remove(session);
    }

    @OnWebSocketError
    public void onError(Session session, Throwable error) {
        logger.error("WebSocket error for session {}", session.getRemoteAddress(), error);
    }

    /**
     * Handles subscription request.
     */
    private void handleSubscribe(String simulationId) {
        // Unsubscribe from previous simulation if any
        if (subscribedSimulationId != null) {
            try {
                SimulationEngine oldEngine = SimulationService.getInstance().getEngine(subscribedSimulationId);
                oldEngine.removeStateListener(this);
            } catch (Exception e) {
                // Ignore
            }
        }

        // Subscribe to new simulation
        try {
            SimulationEngine engine = SimulationService.getInstance().getEngine(simulationId);
            engine.addStateListener(this);

            subscribedSimulationId = simulationId;
            sessionSubscriptions.put(session, simulationId);

            logger.info("Client subscribed to simulation: {}", simulationId);

            // Send confirmation
            Map<String, Object> confirmation = Map.of(
                "type", "subscribed",
                "simulationId", simulationId
            );
            sendMessage(JsonUtil.toJsonCompact(confirmation));

        } catch (Exception e) {
            logger.error("Error subscribing to simulation {}", simulationId, e);

            // Send error
            Map<String, Object> error = Map.of(
                "type", "error",
                "message", "Failed to subscribe: " + e.getMessage()
            );
            sendMessage(JsonUtil.toJsonCompact(error));
        }
    }

    /**
     * Sends message to client.
     */
    private void sendMessage(String message) {
        if (session != null && session.isOpen()) {
            try {
                session.getRemote().sendString(message);
            } catch (IOException e) {
                logger.error("Error sending message to client", e);
            }
        }
    }

    /**
     * StateListener implementation - called when simulation state updates.
     */
    @Override
    public void onStateUpdate(String simulationId, SimulationState state) {
        // Only send if this session is subscribed to this simulation
        if (simulationId.equals(subscribedSimulationId)) {
            // Create broadcast message
            Map<String, Object> message = Map.of(
                "type", "state_update",
                "simulationId", simulationId,
                "tick", state.getTick(),
                "vehicles", state.getVehicles(),
                "staticObjects", state.getStaticObjects()
            );

            String json = JsonUtil.toJsonCompact(message);
            sendMessage(json);
        }
    }
}
