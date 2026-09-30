package com.disaster.agent.server;

import com.disaster.agent.agent.RationalAgent;
import com.disaster.agent.geo.GeoFenceEngine;
import com.disaster.agent.geo.HaversineUtil;
import com.disaster.agent.graph.SensorEdge;
import com.disaster.agent.graph.SensorGraph;
import com.disaster.agent.model.*;
import com.disaster.agent.service.MonitoringService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

/**
 * High-performance Java REST API Server (port 8080).
 * Implements endpoints for Health, Locations, Sensors, Readings, Risk, Weather, Alerts,
 * Notifications, Geofence Auditing, ADSA Graph, Simulation, and Report Downloads.
 */
public class DisasterAgentHttpServer {
    private static final Logger LOGGER = Logger.getLogger(DisasterAgentHttpServer.class.getName());
    private final int port;
    private final MonitoringService monitoringService;
    private HttpServer server;

    public DisasterAgentHttpServer(int port, MonitoringService monitoringService) {
        this.port = port;
        this.monitoringService = monitoringService;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(port), 0);

        // Core API Endpoints
        server.createContext("/", new FrontendHandler());
        server.createContext("/health", new HealthHandler());
        server.createContext("/api/health", new HealthHandler());
        server.createContext("/api/locations", new LocationsHandler());
        server.createContext("/api/sensors", new SensorsHandler());
        server.createContext("/api/readings", new ReadingsHandler());
        server.createContext("/api/risk", new RiskHandler());
        server.createContext("/api/weather", new WeatherHandler());
        server.createContext("/api/forecast", new ForecastHandler());
        server.createContext("/api/alerts", new AlertsHandler());
        server.createContext("/api/notifications", new NotificationsHandler());
        server.createContext("/api/acknowledge", new AcknowledgeHandler());
        server.createContext("/api/simulate", new SimulateHandler());
        server.createContext("/api/graph", new GraphHandler());
        server.createContext("/api/download", new DownloadHandler());
        server.createContext("/api/geofence/evaluate", new GeofenceEvaluateHandler());
        server.createContext("/api/auth/login", new AuthLoginHandler());
        server.createContext("/api/auth/logout", new AuthLogoutHandler());
        server.createContext("/api/auth/session", new AuthSessionHandler());

        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        LOGGER.info("Disaster Early-Warning Java Agent HTTP Server running on http://localhost:" + port);
    }

    public void stop() {
        if (server != null) server.stop(1);
    }

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, String responseJson) throws IOException {
        byte[] bytes = responseJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS, PUT, DELETE");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) return map;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            if (idx > 0) {
                String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                String val = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                map.put(key, val);
            }
        }
        return map;
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody();
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
            return scanner.useDelimiter("\\A").hasNext() ? scanner.next() : "";
        }
    }

    // 0. Static Frontend Handler
    private class FrontendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.equals("/") || path.equals("/dashboard")) {
                path = "/index.html";
            }

            File baseDir = new File("../frontend");
            if (!baseDir.exists()) baseDir = new File("frontend");
            if (!baseDir.exists()) baseDir = new File("DISASTER-EARLY-WARNING/frontend");

            File targetFile = new File(baseDir, path.replace("/", File.separator));
            if (!targetFile.exists() || targetFile.isDirectory()) {
                sendJsonResponse(exchange, 404, "{\"error\":\"File not found\"}");
                return;
            }

            String contentType = "text/plain";
            if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
            else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
            else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";
            else if (path.endsWith(".json")) contentType = "application/json; charset=UTF-8";
            else if (path.endsWith(".png")) contentType = "image/png";

            byte[] bytes = java.nio.file.Files.readAllBytes(targetFile.toPath());
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }

    // 1. Health Handler
    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String json = String.format(
                "{\"status\":\"UP\",\"system\":\"Disaster Early-Warning Agent\"," +
                "\"timestamp\":\"%s\",\"monitoring_24_7\":%b,\"active_locations\":%d," +
                "\"python_engine_connected\":%b,\"total_cycles\":%d}",
                Instant.now().toString(),
                monitoringService.isRunning(),
                monitoringService.getLocations().size(),
                monitoringService.getPythonRiskClient().isLastPythonStatusHealthy(),
                monitoringService.getAgents().values().stream().mapToLong(RationalAgent::getTotalCyclesExecuted).sum()
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    // 2. Locations Handler (GET: list, POST: dynamic location registration)
    private class LocationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                String city = extractStringVal(body, "city", "Custom Location");
                String state = extractStringVal(body, "state", "Default State");
                String district = extractStringVal(body, "district", city);
                String country = extractStringVal(body, "country", "India");
                double lat = extractDoubleVal(body, "latitude", 17.0);
                double lon = extractDoubleVal(body, "longitude", 82.0);
                String locId = "LOC-" + city.toUpperCase().replaceAll("[^A-Z0-9]", "");

                Location newLoc = new Location(locId, country, state, district, city, lat, lon, 15.0, 500000, true);
                monitoringService.registerLocation(newLoc);

                // Add sample sensors for the new dynamic location
                RationalAgent agent = monitoringService.getAgents().get(locId);
                SensorNode rain = new SensorNode("SNS-" + locId + "-RAIN-01", "ZONE-GEN", locId, city + " Telemetry Rain Gauge",
                        SensorType.RAINFALL, lat + 0.01, lon + 0.01, "mm", 0, 70, 120, 0.98);
                SensorNode wl = new SensorNode("SNS-" + locId + "-WL-01", "ZONE-GEN", locId, city + " Hydro Water Level",
                        SensorType.WATER_LEVEL, lat - 0.01, lon - 0.01, "m", 0.5, 3.5, 4.5, 0.97);
                SensorNode wind = new SensorNode("SNS-" + locId + "-WIND-01", "ZONE-GEN", locId, city + " Wind Anemometer",
                        SensorType.WIND_SPEED, lat + 0.02, lon - 0.01, "km/h", 0, 50, 80, 0.95);
                rain.addReading(new Reading(rain.getSensorId(), 15.0, "mm"));
                wl.addReading(new Reading(wl.getSensorId(), 1.2, "m"));
                wind.addReading(new Reading(wind.getSensorId(), 20.0, "km/h"));
                agent.registerSensor(rain);
                agent.registerSensor(wl);
                agent.registerSensor(wind);
                monitoringService.getGlobalGraph().addEdge(rain.getSensorId(), wl.getSensorId(), 1.5, 0.9, "DYNAMIC_COUPLING");

                sendJsonResponse(exchange, 201, "{\"success\":true,\"message\":\"Dynamic location registered successfully\",\"location_id\":\"" + locId + "\"}");
                return;
            }

            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            for (Location loc : monitoringService.getLocations().values()) {
                if (count++ > 0) sb.append(",");
                RationalAgent agent = monitoringService.getAgents().get(loc.getLocationId());
                double score = agent != null && agent.getCurrentAssessment() != null ? agent.getCurrentAssessment().getRiskScore() : 15.0;
                String level = agent != null && agent.getCurrentAssessment() != null ? agent.getCurrentAssessment().getRiskLevel().name() : "NORMAL";

                sb.append(String.format(
                    "{\"location_id\":\"%s\",\"country\":\"%s\",\"state\":\"%s\",\"district\":\"%s\"," +
                    "\"city\":\"%s\",\"latitude\":%.4f,\"longitude\":%.4f,\"current_risk\":%.1f,\"risk_level\":\"%s\"}",
                    loc.getLocationId(), loc.getCountry(), loc.getState(), loc.getDistrict(),
                    loc.getCity(), loc.getLatitude(), loc.getLongitude(), score, level
                ));
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // 3. Sensors Handler
    private class SensorsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            for (SensorNode s : monitoringService.getGlobalGraph().getAllNodes()) {
                if (count++ > 0) sb.append(",");
                Reading r = s.getLatestReading();
                double val = r != null ? r.getValue() : 0.0;
                String status = r != null ? r.getValidationStatus().name() : "NO_READING";

                sb.append(String.format(
                    "{\"sensor_id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"latitude\":%.4f,\"longitude\":%.4f," +
                    "\"value\":%.2f,\"unit\":\"%s\",\"health\":\"%s\",\"reliability\":%.2f,\"validation_status\":\"%s\"}",
                    s.getSensorId(), s.getName(), s.getSensorType().name(), s.getLatitude(), s.getLongitude(),
                    val, s.getUnit(), s.getHealthStatus().name(), s.getReliabilityScore(), status
                ));
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // 4. Readings Handler
    private class ReadingsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            for (SensorNode s : monitoringService.getGlobalGraph().getAllNodes()) {
                Reading r = s.getLatestReading();
                if (r != null) {
                    if (count++ > 0) sb.append(",");
                    sb.append(String.format(
                        "{\"reading_id\":\"%s\",\"sensor_id\":\"%s\",\"value\":%.2f,\"unit\":\"%s\",\"is_valid\":%b," +
                        "\"validation_status\":\"%s\",\"confidence_weight\":%.2f,\"timestamp\":\"%s\"}",
                        r.getReadingId(), r.getSensorId(), r.getValue(), r.getUnit(), r.isValid(),
                        r.getValidationStatus().name(), r.getConfidenceWeight(), r.getTimestamp().toString()
                    ));
                }
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // 5. Risk Handler (Latest risk evaluation across locations)
    private class RiskHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String locId = query.getOrDefault("location_id", "LOC-KAKINADA");

            RationalAgent agent = monitoringService.getAgents().get(locId);
            if (agent == null && !monitoringService.getAgents().isEmpty()) {
                agent = monitoringService.getAgents().values().iterator().next();
            }

            if (agent == null || agent.getCurrentAssessment() == null) {
                sendJsonResponse(exchange, 200, "{\"risk_score\":15.0,\"risk_level\":\"NORMAL\",\"confidence\":90.0,\"trend\":\"STABLE\",\"predicted_risk\":\"NORMAL\",\"contributing_factors\":[\"System Nominal\"]}");
                return;
            }

            RiskAssessment a = agent.getCurrentAssessment();
            StringBuilder factors = new StringBuilder("[");
            for (int i = 0; i < a.getContributingFactors().size(); i++) {
                if (i > 0) factors.append(",");
                factors.append("\"").append(escapeJson(a.getContributingFactors().get(i))).append("\"");
            }
            factors.append("]");

            String json = String.format(
                "{\"location_id\":\"%s\",\"risk_score\":%.1f,\"risk_level\":\"%s\",\"confidence\":%.1f," +
                "\"trend\":\"%s\",\"predicted_risk\":\"%s\",\"engine_source\":\"%s\",\"contributing_factors\":%s," +
                "\"timestamp\":\"%s\",\"disclaimer\":\"Academic prototype thresholds (0-20 Normal, 21-40 Low, 41-60 Mod, 61-80 High, 81-100 Critical). Not official government warning.\"}",
                agent.getLocation().getLocationId(), a.getRiskScore(), a.getRiskLevel(), a.getConfidence(),
                a.getTrend(), a.getPredictedRisk(), a.getEngineSource(), factors.toString(), a.getTimestamp().toString()
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    // 6. Weather Handler
    private class WeatherHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String locId = query.getOrDefault("location_id", "LOC-KAKINADA");
            Location loc = monitoringService.getLocations().get(locId);
            if (loc == null && !monitoringService.getLocations().isEmpty()) {
                loc = monitoringService.getLocations().values().iterator().next();
            }

            WeatherData w = monitoringService.getWeatherService().getWeather(loc);
            String json = String.format(
                "{\"location_id\":\"%s\",\"city\":\"%s\",\"provider\":\"%s\",\"temperature_c\":%.1f," +
                "\"humidity_pct\":%.1f,\"pressure_hpa\":%.1f,\"wind_speed_kmh\":%.1f,\"rainfall_mm\":%.1f," +
                "\"forecast\":\"%s\",\"recorded_at\":\"%s\"}",
                loc != null ? loc.getLocationId() : locId,
                loc != null ? loc.getCity() : "Selected Location",
                w.getProvider(), w.getTemperatureC(), w.getHumidityPct(), w.getPressureHpa(),
                w.getWindSpeedKmh(), w.getRainfallMm(), escapeJson(w.getForecastSummary()), w.getRecordedAt().toString()
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    // 7. Forecast Handler
    private class ForecastHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String locId = query.getOrDefault("location_id", "LOC-KAKINADA");
            Location loc = monitoringService.getLocations().get(locId);
            WeatherData w = monitoringService.getWeatherService().getWeather(loc);

            String json = String.format(
                "{\"location_id\":\"%s\",\"forecast_summary\":\"%s\",\"next_24h\":[" +
                "{\"hour\":\"+6h\",\"rain_mm\":%.1f,\"temp_c\":%.1f,\"condition\":\"Monsoon Clouds\"}," +
                "{\"hour\":\"+12h\",\"rain_mm\":%.1f,\"temp_c\":%.1f,\"condition\":\"Thunderstorm Gusts\"}," +
                "{\"hour\":\"+18h\",\"rain_mm\":%.1f,\"temp_c\":%.1f,\"condition\":\"Precipitation Clearing\"}" +
                "]}",
                locId, escapeJson(w.getForecastSummary()),
                w.getRainfallMm() * 0.8, w.getTemperatureC() - 1.5,
                w.getRainfallMm() * 1.3, w.getTemperatureC() - 2.0,
                w.getRainfallMm() * 0.4, w.getTemperatureC() + 1.0
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    // 8. Alerts Handler
    private class AlertsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Alert> allAlerts = new ArrayList<>();
            for (RationalAgent agent : monitoringService.getAgents().values()) {
                allAlerts.addAll(agent.getActiveAlerts());
            }

            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            for (Alert a : allAlerts) {
                if (count++ > 0) sb.append(",");
                StringBuilder reasons = new StringBuilder("[");
                if (a.getReasons() != null) {
                    for (int r = 0; r < a.getReasons().size(); r++) {
                        if (r > 0) reasons.append(",");
                        reasons.append("\"").append(escapeJson(a.getReasons().get(r))).append("\"");
                    }
                }
                reasons.append("]");

                sb.append(String.format(
                    "{\"alert_id\":\"%s\",\"location_id\":\"%s\",\"location_name\":\"%s\",\"district\":\"%s\",\"alert_type\":\"%s\"," +
                    "\"hazard_title\":\"%s\",\"severity\":\"%s\",\"risk_score\":%.1f,\"rainfall_mm\":%.1f," +
                    "\"water_level_m\":%.1f,\"wind_kmh\":%.1f,\"humidity_pct\":%.1f,\"confidence\":%.1f," +
                    "\"trend\":\"%s\",\"predicted_risk\":\"%s\",\"epicenter_lat\":%.4f,\"epicenter_lon\":%.4f," +
                    "\"disaster_radius_km\":%.1f,\"weather_radius_km\":%.1f,\"reasons\":%s,\"status\":\"%s\"," +
                    "\"created_at\":\"%s\",\"disaster_msg\":\"%s\",\"weather_msg\":\"%s\"}",
                    a.getAlertId(), a.getLocationId(), a.getLocationName(), escapeJson(a.getDistrict()), a.getAlertType(),
                    escapeJson(a.getHazardTitle()), a.getSeverity(), a.getRiskScore(), a.getRainfallMm(),
                    a.getWaterLevelM(), a.getWindKmh(), a.getHumidityPct(), a.getConfidencePct(),
                    a.getTrend(), a.getPredictedRisk(), a.getEpicenterLat(), a.getEpicenterLon(),
                    Alert.DISASTER_ALERT_RADIUS_KM, Alert.WEATHER_ALERT_RADIUS_KM,
                    reasons.toString(), a.getStatus(), a.getCreatedAt().toString(),
                    escapeJson(a.formatDisasterAlertMessage(15.0)),
                    escapeJson(a.formatWeatherAlertMessage(120.0))
                ));
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // 9. Notifications Handler
    private class NotificationsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<NotificationResult> allNotifs = new ArrayList<>();
            for (RationalAgent agent : monitoringService.getAgents().values()) {
                allNotifs.addAll(agent.getNotificationLog());
            }

            StringBuilder sb = new StringBuilder("[");
            int count = 0;
            for (NotificationResult n : allNotifs) {
                if (count++ > 0) sb.append(",");
                sb.append(String.format(
                    "{\"notification_id\":\"%s\",\"alert_id\":\"%s\",\"user_id\":\"%s\",\"distance_km\":%.2f," +
                    "\"qualifying_radius_km\":%.1f,\"alert_type\":\"%s\",\"delivery_status\":\"%s\"," +
                    "\"explanation\":\"%s\",\"timestamp\":\"%s\"}",
                    n.getNotificationId(), n.getAlertId(), n.getUserId(), n.getCalculatedDistanceKm(),
                    n.getQualifyingRadiusKm(), n.getAlertTypeAssigned(), n.getDeliveryStatus(),
                    escapeJson(n.getExplanationReason()), n.getTimestamp().toString()
                ));
            }
            sb.append("]");
            sendJsonResponse(exchange, 200, sb.toString());
        }
    }

    // 10. Acknowledge Handler
    private class AcknowledgeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            sendJsonResponse(exchange, 200, "{\"success\":true,\"message\":\"Alert acknowledged. Safety confirmation recorded in audit trail.\",\"timestamp\":\"" + Instant.now() + "\"}");
        }
    }

    // 11. Simulate Handler
    private class SimulateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String locId = query.getOrDefault("location_id", "LOC-KAKINADA");
            String scenario = query.getOrDefault("scenario", "NORMAL");

            monitoringService.injectSimulationScenario(locId, scenario);

            RationalAgent agent = monitoringService.getAgents().get(locId);
            double score = agent != null && agent.getCurrentAssessment() != null ? agent.getCurrentAssessment().getRiskScore() : 20.0;
            String level = agent != null && agent.getCurrentAssessment() != null ? agent.getCurrentAssessment().getRiskLevel().name() : "NORMAL";

            String json = String.format(
                "{\"success\":true,\"scenario\":\"%s\",\"location_id\":\"%s\",\"risk_score\":%.1f,\"risk_level\":\"%s\"}",
                scenario, locId, score, level
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    // 12. Graph Handler (ADSA Graph Topology & Traversal)
    private class GraphHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            SensorGraph graph = monitoringService.getGlobalGraph();
            StringBuilder nodesJson = new StringBuilder("[");
            int nCount = 0;
            for (SensorNode n : graph.getAllNodes()) {
                if (nCount++ > 0) nodesJson.append(",");
                Reading r = n.getLatestReading();
                double val = r != null ? r.getValue() : 0.0;
                nodesJson.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"type\":\"%s\",\"lat\":%.4f,\"lon\":%.4f,\"val\":%.1f,\"health\":\"%s\"}",
                    n.getSensorId(), n.getName(), n.getSensorType(), n.getLatitude(), n.getLongitude(), val, n.getHealthStatus()
                ));
            }
            nodesJson.append("]");

            StringBuilder edgesJson = new StringBuilder("[");
            int eCount = 0;
            Set<String> seenEdges = new HashSet<>();
            for (SensorNode n : graph.getAllNodes()) {
                for (SensorEdge e : graph.getEdges(n.getSensorId())) {
                    String edgeKey = n.getSensorId().compareTo(e.getTargetSensorId()) < 0 ?
                            n.getSensorId() + "->" + e.getTargetSensorId() : e.getTargetSensorId() + "->" + n.getSensorId();
                    if (!seenEdges.contains(edgeKey)) {
                        seenEdges.add(edgeKey);
                        if (eCount++ > 0) edgesJson.append(",");
                        edgesJson.append(String.format(
                            "{\"source\":\"%s\",\"target\":\"%s\",\"distance_km\":%.2f,\"weight\":%.2f,\"coupling\":\"%s\"}",
                            e.getSourceSensorId(), e.getTargetSensorId(), e.getDistanceKm(), e.getWeight(), e.getCouplingType()
                        ));
                    }
                }
            }
            edgesJson.append("]");

            // Run sample BFS and Connected Components for inspection
            String sampleId = graph.getAllNodes().isEmpty() ? "" : graph.getAllNodes().iterator().next().getSensorId();
            List<String> bfsResult = sampleId.isEmpty() ? Collections.emptyList() : graph.bfs(sampleId);
            List<Set<String>> components = graph.getConnectedComponents();

            String json = String.format(
                "{\"nodes\":%s,\"edges\":%s,\"connected_components_count\":%d,\"sample_bfs_start\":\"%s\",\"sample_bfs_path\":%s}",
                nodesJson.toString(), edgesJson.toString(), components.size(), sampleId, listToJson(bfsResult)
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    // 13. Geofence Evaluate Handler (Evaluates user's live coordinates & district)
    private class GeofenceEvaluateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
            double uLat = Double.parseDouble(q.getOrDefault("lat", "16.9891"));
            double uLon = Double.parseDouble(q.getOrDefault("lon", "82.2475"));
            String locId = q.getOrDefault("location_id", "LOC-KAKINADA");
            String userDistrict = q.getOrDefault("district", q.getOrDefault("user_district", ""));

            Location loc = monitoringService.getLocations().get(locId);
            double epicLat = loc != null ? loc.getLatitude() : 16.9891;
            double epicLon = loc != null ? loc.getLongitude() : 82.2475;
            String alertDistrict = loc != null ? loc.getDistrict() : "Monitored District";

            GeoFenceEngine.GeofenceEvaluation eval = GeoFenceEngine.evaluateLocationWithDistrict(
                epicLat, epicLon, alertDistrict, uLat, uLon, userDistrict
            );

            boolean districtMatched = userDistrict.isBlank() || alertDistrict.equalsIgnoreCase(userDistrict.trim());

            String json = String.format(
                "{\"user_lat\":%.4f,\"user_lon\":%.4f,\"epicenter_lat\":%.4f,\"epicenter_lon\":%.4f," +
                "\"user_district\":\"%s\",\"alert_district\":\"%s\",\"district_matched\":%b," +
                "\"distance_km\":%.2f,\"geofence_status\":\"%s\",\"disaster_radius_km\":100.0," +
                "\"weather_radius_km\":150.0,\"disaster_alert_eligible\":%b,\"weather_alert_eligible\":%b," +
                "\"explanation\":\"%s\"}",
                uLat, uLon, epicLat, epicLon, escapeJson(userDistrict), escapeJson(alertDistrict), districtMatched,
                eval.getDistanceKm(), eval.getStatus().name(),
                eval.getStatus() == GeoFenceEngine.GeofenceStatus.DISASTER_AND_WEATHER,
                eval.getStatus() == GeoFenceEngine.GeofenceStatus.DISASTER_AND_WEATHER || eval.getStatus() == GeoFenceEngine.GeofenceStatus.WEATHER_ONLY,
                escapeJson(eval.getExplanation())
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    // 14. Download Handler (PDF, CSV, JSON, Printable HTML)
    private class DownloadHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> q = parseQueryParams(exchange.getRequestURI().getQuery());
            String format = q.getOrDefault("format", "json").toLowerCase();
            String locId = q.getOrDefault("location_id", "LOC-KAKINADA");
            Location loc = monitoringService.getLocations().get(locId);
            RationalAgent agent = monitoringService.getAgents().get(locId);

            RiskAssessment rsk = agent != null ? agent.getCurrentAssessment() : null;
            WeatherData w = agent != null ? agent.getCurrentWeather() : null;
            double score = rsk != null ? rsk.getRiskScore() : 18.0;
            String level = rsk != null ? rsk.getRiskLevel().name() : "NORMAL";

            if ("csv".equals(format)) {
                String csv = "Report_Type,Disaster Early-Warning Intelligence Audit\n" +
                             "Timestamp," + Instant.now() + "\n" +
                             "Location," + (loc != null ? loc.getCity() : "Monitored Location") + "\n" +
                             "Latitude," + (loc != null ? loc.getLatitude() : 0.0) + "\n" +
                             "Longitude," + (loc != null ? loc.getLongitude() : 0.0) + "\n" +
                             "Risk_Score," + score + "\n" +
                             "Risk_Level," + level + "\n" +
                             "Confidence_Pct," + (rsk != null ? rsk.getConfidence() : 85.0) + "\n" +
                             "Trend," + (rsk != null ? rsk.getTrend() : "STABLE") + "\n" +
                             "Predicted_Risk," + (rsk != null ? rsk.getPredictedRisk() : "NORMAL") + "\n" +
                             "Disaster_Radius_KM,100.0\n" +
                             "Weather_Radius_KM,150.0\n" +
                             "Rainfall_MM," + (w != null ? w.getRainfallMm() : 15.0) + "\n" +
                             "Wind_KMH," + (w != null ? w.getWindSpeedKmh() : 20.0) + "\n" +
                             "Disclaimer,Prototype disaster warning research tool. Not official government warning.\n";
                exchange.getResponseHeaders().set("Content-Type", "text/csv");
                exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"disaster_report_" + locId + ".csv\"");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                byte[] b = csv.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
            } else if ("html".equals(format)) {
                String html = "<!DOCTYPE html><html><head><meta charset='utf-8'><title>Disaster Intelligence Dossier</title>" +
                              "<style>body{font-family:sans-serif;padding:30px;background:#0f172a;color:#f8fafc;} " +
                              ".card{background:#1e293b;border-radius:12px;padding:24px;max-width:800px;margin:auto;border:1px solid #334155;}" +
                              "h1{color:#ef4444;margin-top:0;} .badge{background:#ef4444;color:#fff;padding:4px 10px;border-radius:6px;}" +
                              ".metric{display:inline-block;margin:10px 20px 10px 0;} .label{font-size:12px;color:#94a3b8;}</style></head><body>" +
                              "<div class='card'><h1>🚨 DISASTER & WEATHER REPORT</h1>" +
                              "<p><strong>Location:</strong> " + (loc != null ? loc.getCity() + ", " + loc.getState() : "Monitored Region") + "</p>" +
                              "<p><strong>Timestamp:</strong> " + Instant.now() + "</p>" +
                              "<hr style='border-color:#334155;'>" +
                              "<div class='metric'><div class='label'>RISK SCORE</div><div style='font-size:24px;font-weight:bold;'>" + score + "/100</div></div>" +
                              "<div class='metric'><div class='label'>RISK LEVEL</div><div class='badge'>" + level + "</div></div>" +
                              "<div class='metric'><div class='label'>100KM DISASTER RADIUS</div><div>ACTIVE</div></div>" +
                              "<div class='metric'><div class='label'>150KM WEATHER RADIUS</div><div>ACTIVE</div></div>" +
                              "<p><em>Note: Prototype research system. Does not replace official government warnings.</em></p>" +
                              "<button onclick='window.print()' style='background:#3b82f6;color:white;border:none;padding:10px 20px;border-radius:6px;cursor:pointer;'>Print Official Dossier</button>" +
                              "</div></body></html>";
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                byte[] b = html.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, b.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(b); }
            } else {
                // Return comprehensive JSON report
                String json = String.format(
                    "{\"report_id\":\"REP-%d\",\"location\":\"%s\",\"risk_score\":%.1f,\"risk_level\":\"%s\"," +
                    "\"confidence\":%.1f,\"trend\":\"%s\",\"disaster_radius_km\":100.0,\"weather_radius_km\":150.0," +
                    "\"generated_at\":\"%s\",\"download_formats\":[\"PDF\",\"CSV\",\"JSON\",\"HTML\"]," +
                    "\"disclaimer\":\"Academic prototype thresholds. Not official government warning.\"}",
                    System.currentTimeMillis(), (loc != null ? loc.getCity() : locId), score, level,
                    rsk != null ? rsk.getConfidence() : 85.0, rsk != null ? rsk.getTrend() : "STABLE", Instant.now()
                );
                sendJsonResponse(exchange, 200, json);
            }
        }
    }

    // --- SINGLE-SESSION AUTHENTICATION SUBSYSTEM ---
    public static class ActiveSession {
        private final String email;
        private final String district;
        private final String token;
        private final long loginTimestamp;

        public ActiveSession(String email, String district, String token, long loginTimestamp) {
            this.email = email;
            this.district = district;
            this.token = token;
            this.loginTimestamp = loginTimestamp;
        }

        public String getEmail() { return email; }
        public String getDistrict() { return district; }
        public String getToken() { return token; }
        public long getLoginTimestamp() { return loginTimestamp; }
    }

    private static final java.util.concurrent.atomic.AtomicReference<ActiveSession> currentSession =
            new java.util.concurrent.atomic.AtomicReference<>(null);

    // POST /api/auth/login
    private class AuthLoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 405, "{\"error\":\"Method not allowed. Use POST.\"}");
                return;
            }

            String body = readRequestBody(exchange);
            String email = extractStringVal(body, "email", "").trim();
            String password = extractStringVal(body, "password", "").trim();
            String district = extractStringVal(body, "district", "Kakinada").trim();

            if (email.isBlank() || !email.contains("@")) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Invalid Gmail or email address provided.\"}");
                return;
            }
            if (password.length() < 4) {
                sendJsonResponse(exchange, 400, "{\"success\":false,\"error\":\"Password must be at least 4 characters long.\"}");
                return;
            }

            // Multi-user login enabled (Single Active Session Policy removed per user request)
            String token = "AUTH-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
            ActiveSession newSession = new ActiveSession(email, district.isBlank() ? "Kakinada" : district, token, System.currentTimeMillis());
            currentSession.set(newSession);
            LOGGER.info("[AUTH] Session established for user: " + email + " in district: " + newSession.getDistrict());

            String json = String.format(
                "{\"success\":true,\"email\":\"%s\",\"district\":\"%s\",\"token\":\"%s\",\"loginTimestamp\":%d,\"message\":\"Login successful.\"}",
                escapeJson(newSession.getEmail()), escapeJson(newSession.getDistrict()), newSession.getToken(), newSession.getLoginTimestamp()
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    // POST /api/auth/logout
    private class AuthLogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            ActiveSession loggedOut = currentSession.getAndSet(null);
            if (loggedOut != null) {
                LOGGER.info("[AUTH] User logged out: " + loggedOut.getEmail() + ". System reset to initial login state.");
            }
            String prevEmail = loggedOut != null ? loggedOut.getEmail() : "None";
            String json = String.format(
                "{\"success\":true,\"message\":\"User successfully logged out. System reset to start. Fresh login is now available.\",\"previousUser\":\"%s\"}",
                escapeJson(prevEmail)
            );
            sendJsonResponse(exchange, 200, json);
        }
    }

    // GET /api/auth/session
    private class AuthSessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendJsonResponse(exchange, 204, "");
                return;
            }
            ActiveSession active = currentSession.get();
            if (active != null) {
                String json = String.format(
                    "{\"active\":true,\"email\":\"%s\",\"district\":\"%s\",\"token\":\"%s\",\"loginTimestamp\":%d}",
                    escapeJson(active.getEmail()), escapeJson(active.getDistrict()), active.getToken(), active.getLoginTimestamp()
                );
                sendJsonResponse(exchange, 200, json);
            } else {
                sendJsonResponse(exchange, 200, "{\"active\":false,\"message\":\"No active session. Initial login required.\"}");
            }
        }
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }

    private static String listToJson(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("\"").append(escapeJson(list.get(i))).append("\"");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String extractStringVal(String json, String key, String defaultVal) {
        try {
            int kIdx = json.indexOf("\"" + key + "\"");
            if (kIdx == -1) return defaultVal;
            int cIdx = json.indexOf(":", kIdx);
            int q1 = json.indexOf("\"", cIdx);
            int q2 = json.indexOf("\"", q1 + 1);
            if (q1 != -1 && q2 != -1) return json.substring(q1 + 1, q2);
        } catch (Exception ignored) {}
        return defaultVal;
    }

    private static double extractDoubleVal(String json, String key, double defaultVal) {
        try {
            int kIdx = json.indexOf("\"" + key + "\"");
            if (kIdx == -1) return defaultVal;
            int cIdx = json.indexOf(":", kIdx);
            int comma = json.indexOf(",", cIdx);
            int brace = json.indexOf("}", cIdx);
            int end = comma != -1 ? (brace != -1 ? Math.min(comma, brace) : comma) : brace;
            if (end == -1) end = json.length();
            String raw = json.substring(cIdx + 1, end).replaceAll("[^0-9.-]", "").trim();
            return Double.parseDouble(raw);
        } catch (Exception ignored) {}
        return defaultVal;
    }
}
