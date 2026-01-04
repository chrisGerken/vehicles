package com.gerkenip.vehicles;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.websocket.server.config.JettyWebSocketServletContainerInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * Main application entry point.
 * Starts embedded Jetty server with servlets and WebSocket support.
 */
public class Application {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);
    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            Application app = new Application();
            app.start();
        } catch (Exception e) {
            logger.error("Failed to start application", e);
            System.exit(1);
        }
    }

    public void start() throws Exception {
        logger.info("Starting Vehicle Simulation Server on port {}", PORT);

        // Create Jetty server
        Server server = new Server(PORT);

        // Create servlet context
        ServletContextHandler servletContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
        servletContext.setContextPath("/");

        // Register servlets
        servletContext.addServlet(com.gerkenip.vehicles.servlet.SimulationServlet.class, "/api/simulation/*");

        // Configure WebSocket support
        JettyWebSocketServletContainerInitializer.configure(servletContext, (servletContext1, wsContainer) -> {
            // Set WebSocket policy
            wsContainer.setMaxTextMessageSize(65536);
            wsContainer.setIdleTimeout(Duration.ofMinutes(10));

            // Register WebSocket endpoints
            wsContainer.addMapping("/ws", com.gerkenip.vehicles.websocket.SimulationWebSocket.class);
        });

        // Create resource handler for static files (frontend)
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setDirectoriesListed(false);
        resourceHandler.setWelcomeFiles(new String[]{"index.html"});
        resourceHandler.setResourceBase("frontend");

        // Combine handlers
        HandlerList handlers = new HandlerList();
        handlers.addHandler(resourceHandler);
        handlers.addHandler(servletContext);

        server.setHandler(handlers);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down server...");
            try {
                server.stop();
            } catch (Exception e) {
                logger.error("Error during shutdown", e);
            }
        }));

        // Start server
        server.start();
        logger.info("Server started successfully at http://localhost:{}", PORT);
        logger.info("Frontend available at http://localhost:{}/", PORT);
        logger.info("API available at http://localhost:{}/api/*", PORT);
        logger.info("WebSocket available at ws://localhost:{}/ws", PORT);

        // Wait for server to complete
        server.join();
    }
}
