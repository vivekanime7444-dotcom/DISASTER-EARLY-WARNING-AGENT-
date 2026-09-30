package com.disaster.agent;

import com.disaster.agent.server.DisasterAgentHttpServer;
import com.disaster.agent.service.MonitoringService;

import java.util.logging.Logger;

/**
 * Universal Disaster Early-Warning & Weather Intelligence Agent
 * Main OOPJ Application Entry Point.
 */
public class DisasterAgentApp {
    private static final Logger LOGGER = Logger.getLogger(DisasterAgentApp.class.getName());
    public static final int DEFAULT_PORT = 8080;

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("==========================================================================");
        System.out.println("  UNIVERSAL DISASTER EARLY-WARNING & WEATHER INTELLIGENCE AGENT (OOPJ/ADSA)");
        System.out.println("  AI Rational Agent + ADSA Sensor Graph + Python Risk Engine + Dual Radii");
        System.out.println("==========================================================================");

        try {
            // 1. Initialize 24/7 Monitoring Service with ADSA Sensor Graph
            MonitoringService monitoringService = new MonitoringService();

            // 2. Start Continuous 24/7 Monitoring loop
            monitoringService.start24x7Monitoring();

            // 3. Start REST HTTP Server
            DisasterAgentHttpServer server = new DisasterAgentHttpServer(port, monitoringService);
            server.start();

            System.out.println("  STATUS: 24/7 Continuous Monitoring ACTIVE");
            System.out.println("  REST API Base: http://localhost:" + port);
            System.out.println("  Endpoints:");
            System.out.println("    - GET  /health");
            System.out.println("    - GET  /api/locations (dynamic registry)");
            System.out.println("    - POST /api/locations (add any custom lat/lon)");
            System.out.println("    - GET  /api/sensors");
            System.out.println("    - GET  /api/readings");
            System.out.println("    - GET  /api/risk?location_id=...");
            System.out.println("    - GET  /api/weather?location_id=...");
            System.out.println("    - GET  /api/alerts");
            System.out.println("    - GET  /api/notifications");
            System.out.println("    - GET  /api/graph (ADSA BFS/DFS/Connected Components)");
            System.out.println("    - POST /api/simulate?scenario=CRITICAL&location_id=...");
            System.out.println("    - GET  /api/geofence/evaluate?lat=...&lon=...");
            System.out.println("    - GET  /api/download?format=pdf|csv|json|html");
            System.out.println("==========================================================================");

            // Graceful shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("Initiating graceful shutdown of Disaster Agent...");
                monitoringService.stopMonitoring();
                server.stop();
                LOGGER.info("Disaster Early-Warning Agent shutdown completed.");
            }));

        } catch (Exception e) {
            LOGGER.severe("Fatal error bootstrapping Disaster Early-Warning Agent: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
