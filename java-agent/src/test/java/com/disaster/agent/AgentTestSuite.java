package com.disaster.agent;

import com.disaster.agent.agent.RationalAgent;
import com.disaster.agent.agent.RuleEngine;
import com.disaster.agent.client.PythonRiskClient;
import com.disaster.agent.geo.GeoFenceEngine;
import com.disaster.agent.geo.HaversineUtil;
import com.disaster.agent.graph.SensorGraph;
import com.disaster.agent.model.*;
import com.disaster.agent.weather.WeatherService;

import java.util.*;

/**
 * Automated Verification & Boundary Test Suite.
 * Validates:
 * 1. ADSA Graph Traversal (BFS, DFS, Connected Components, Dijkstra)
 * 2. Spatial Neighbor Correlation Anomaly Detection
 * 3. Haversine Geographic Distance
 * 4. Dual Notification Radii Boundary Rules:
 *    - 100.0 km -> DISASTER_ALERT + WEATHER_ALERT
 *    - 100.1 km -> WEATHER_ALERT ONLY
 *    - 150.0 km -> WEATHER_ALERT ONLY
 *    - 150.1 km -> NO ALERT (SUPPRESSED)
 * 5. Java RuleEngine Deterministic Risk Evaluation & Resilient Fallback
 * 6. Rational Agent 8-Stage Cycle (PERCEIVE->VALIDATE->ANALYZE->REASON->PREDICT->DECIDE->ACT->MONITOR)
 */
public class AgentTestSuite {

    public static void main(String[] args) {
        System.out.println("====================================================================");
        System.out.println(" RUNNING DISASTER EARLY-WARNING SYSTEM AUTOMATED TEST SUITE");
        System.out.println("====================================================================");

        int passed = 0;
        int failed = 0;

        try {
            testADSAAlgorithms();
            System.out.println("  [PASS] Test 1: ADSA Graph (BFS, DFS, Dijkstra, Connected Components)");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 1: ADSA Graph - " + t.getMessage());
            failed++;
        }

        try {
            testNeighborAnomalyDetection();
            System.out.println("  [PASS] Test 2: Spatial Neighbor Correlation & Anomaly Filtering");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 2: Neighbor Correlation - " + t.getMessage());
            failed++;
        }

        try {
            testGeofenceBoundaries();
            System.out.println("  [PASS] Test 3: Dual Notification Radii Exact Boundary Verification (100km / 100.1km / 150km / 150.1km)");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 3: Geofence Boundaries - " + t.getMessage());
            failed++;
        }

        try {
            testJavaRuleEngineFallback();
            System.out.println("  [PASS] Test 4: Java RuleEngine Risk Evaluation & Resilient Fallback");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 4: RuleEngine Fallback - " + t.getMessage());
            failed++;
        }

        try {
            testRationalAgentCycle();
            System.out.println("  [PASS] Test 5: Rational Agent 8-Stage Execution Cycle");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 5: Rational Agent Cycle - " + t.getMessage());
            failed++;
        }

        try {
            testDistrictRestriction();
            System.out.println("  [PASS] Test 6: District-Specific Alert Restriction (Only Alert Sent in User Alert District)");
            passed++;
        } catch (Throwable t) {
            System.err.println("  [FAIL] Test 6: District Restriction - " + t.getMessage());
            failed++;
        }

        System.out.println("====================================================================");
        System.out.println(String.format(" SUMMARY: %d PASSED, %d FAILED (Success Rate: %.1f%%)",
                passed, failed, (passed * 100.0 / (passed + failed))));
        System.out.println("====================================================================");

        if (failed > 0) System.exit(1);
    }

    private static void testADSAAlgorithms() {
        SensorGraph graph = new SensorGraph();
        SensorNode s1 = new SensorNode("S1", "Z1", "L1", "Gauge 1", SensorType.RAINFALL, 16.9, 82.2, "mm", 0, 70, 120, 1.0);
        SensorNode s2 = new SensorNode("S2", "Z1", "L1", "Gauge 2", SensorType.RAINFALL, 16.92, 82.22, "mm", 0, 70, 120, 1.0);
        SensorNode s3 = new SensorNode("S3", "Z1", "L1", "Water Level 1", SensorType.WATER_LEVEL, 16.94, 82.24, "m", 0, 3.5, 4.5, 1.0);
        SensorNode s4 = new SensorNode("S4", "Z2", "L1", "Isolated Anemometer", SensorType.WIND_SPEED, 17.5, 83.0, "km/h", 0, 50, 80, 1.0);

        graph.addNode(s1);
        graph.addNode(s2);
        graph.addNode(s3);
        graph.addNode(s4);

        graph.addEdge("S1", "S2", 2.5, 0.9, "PROXIMITY");
        graph.addEdge("S2", "S3", 3.0, 0.95, "RIVER_FLOW");

        // Test BFS
        List<String> bfs = graph.bfs("S1");
        assert bfs.size() == 3 : "BFS should visit 3 connected nodes";
        assert bfs.get(0).equals("S1") : "BFS root should be S1";

        // Test DFS
        List<String> dfs = graph.dfs("S1");
        assert dfs.size() == 3 : "DFS should visit 3 connected nodes";

        // Test Connected Components
        List<Set<String>> components = graph.getConnectedComponents();
        assert components.size() == 2 : "Should have 2 connected components (S1-S2-S3 and isolated S4)";

        // Test Dijkstra shortest path
        List<String> path = graph.findShortestWeightedPath("S1", "S3");
        assert path.size() == 3 : "Path should be S1 -> S2 -> S3";
        assert path.get(0).equals("S1") && path.get(2).equals("S3");
    }

    private static void testNeighborAnomalyDetection() {
        SensorGraph graph = new SensorGraph();
        SensorNode r1 = new SensorNode("R1", "Z1", "L1", "Rain 1", SensorType.RAINFALL, 16.9, 82.2, "mm", 0, 70, 120, 1.0);
        SensorNode r2 = new SensorNode("R2", "Z1", "L1", "Rain 2", SensorType.RAINFALL, 16.91, 82.21, "mm", 0, 70, 120, 1.0);
        SensorNode wl1 = new SensorNode("WL1", "Z1", "L1", "Water Level 1", SensorType.WATER_LEVEL, 16.905, 82.205, "m", 0, 3.5, 4.5, 1.0);

        graph.addNode(r1);
        graph.addNode(r2);
        graph.addNode(wl1);

        graph.addEdge("R1", "R2", 1.2, 0.9, "PROXIMITY");
        graph.addEdge("R1", "WL1", 0.8, 0.95, "RIVER_BASIN");

        // Case A: R1 spikes to 145mm, but R2 is 5mm and WL1 is 0.8m (Isolated spike -> should NOT be supported)
        r2.addReading(new Reading("R2", 5.0, "mm"));
        wl1.addReading(new Reading("WL1", 0.8, "m"));
        Reading spike = new Reading("R1", 145.0, "mm");
        r1.addReading(spike);

        SensorGraph.NeighborCorrelationResult isolatedResult = graph.validateWithNeighbors("R1", spike);
        assert !isolatedResult.isSupportedByNeighbors() : "Should detect isolated reading when neighbors are nominal";
        assert isolatedResult.getConfidenceWeight() <= 0.5 : "Confidence should be downgraded for isolated spike";

        // Case B: R2 also spikes to 135mm and WL1 rises to 4.6m (Genuine disaster cluster)
        r2.addReading(new Reading("R2", 135.0, "mm"));
        wl1.addReading(new Reading("WL1", 4.6, "m"));

        SensorGraph.NeighborCorrelationResult clusterResult = graph.validateWithNeighbors("R1", spike);
        assert clusterResult.isSupportedByNeighbors() : "Should confirm cluster when neighbors confirm hazard";
        assert clusterResult.getConfirmingCount() >= 2 : "Both R2 and WL1 should confirm";
        assert clusterResult.getConfidenceWeight() >= 0.9 : "Confidence should be boosted for confirmed cluster";
    }

    private static void testGeofenceBoundaries() {
        double epicLat = 16.9891;
        double epicLon = 82.2475;

        // Boundary Test 1: 100.0 km -> DISASTER_ALERT + WEATHER_ALERT
        double[] pt100 = HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 100.0, 90.0);
        GeoFenceEngine.GeofenceEvaluation eval100 = GeoFenceEngine.evaluateLocation(epicLat, epicLon, pt100[0], pt100[1]);
        assert eval100.getStatus() == GeoFenceEngine.GeofenceStatus.DISASTER_AND_WEATHER :
                "Exact 100.0km must trigger Disaster + Weather Alert, got: " + eval100.getStatus();

        // Boundary Test 2: 100.1 km -> WEATHER_ALERT ONLY
        double[] pt100_1 = HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 100.1, 90.0);
        GeoFenceEngine.GeofenceEvaluation eval100_1 = GeoFenceEngine.evaluateLocation(epicLat, epicLon, pt100_1[0], pt100_1[1]);
        assert eval100_1.getStatus() == GeoFenceEngine.GeofenceStatus.WEATHER_ONLY :
                "100.1km must trigger Weather Alert ONLY, got: " + eval100_1.getStatus();

        // Boundary Test 3: 150.0 km -> WEATHER_ALERT ONLY
        double[] pt150 = HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 150.0, 90.0);
        GeoFenceEngine.GeofenceEvaluation eval150 = GeoFenceEngine.evaluateLocation(epicLat, epicLon, pt150[0], pt150[1]);
        assert eval150.getStatus() == GeoFenceEngine.GeofenceStatus.WEATHER_ONLY :
                "Exact 150.0km must trigger Weather Alert ONLY, got: " + eval150.getStatus();

        // Boundary Test 4: 150.1 km -> NO ALERT (SUPPRESSED)
        double[] pt150_1 = HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 150.1, 90.0);
        GeoFenceEngine.GeofenceEvaluation eval150_1 = GeoFenceEngine.evaluateLocation(epicLat, epicLon, pt150_1[0], pt150_1[1]);
        assert eval150_1.getStatus() == GeoFenceEngine.GeofenceStatus.OUT_OF_RANGE :
                "150.1km must be OUT_OF_RANGE / No alert, got: " + eval150_1.getStatus();
    }

    private static void testJavaRuleEngineFallback() {
        Map<SensorType, Double> normalValues = new EnumMap<>(SensorType.class);
        normalValues.put(SensorType.RAINFALL, 18.0);
        normalValues.put(SensorType.WATER_LEVEL, 1.2);
        normalValues.put(SensorType.WIND_SPEED, 20.0);

        SensorGraph.NeighborCorrelationResult normCorr = new SensorGraph.NeighborCorrelationResult(true, 0, 0, "Nominal", 1.0);
        RiskAssessment normalAssessment = RuleEngine.evaluateRisk("LOC-TEST", normalValues, normCorr, null, 20.0);
        assert normalAssessment.getRiskLevel() == RiskLevel.NORMAL : "Low readings must produce NORMAL risk";

        Map<SensorType, Double> critValues = new EnumMap<>(SensorType.class);
        critValues.put(SensorType.RAINFALL, 145.0);
        critValues.put(SensorType.WATER_LEVEL, 4.8);
        critValues.put(SensorType.WIND_SPEED, 62.0);
        critValues.put(SensorType.HUMIDITY, 94.0);

        SensorGraph.NeighborCorrelationResult critCorr = new SensorGraph.NeighborCorrelationResult(true, 3, 3, "Confirmed", 0.95);
        RiskAssessment critAssessment = RuleEngine.evaluateRisk("LOC-TEST", critValues, critCorr, null, 20.0);
        assert critAssessment.getRiskLevel() == RiskLevel.CRITICAL || critAssessment.getRiskLevel() == RiskLevel.HIGH :
                "Critical readings with neighbor confirmation must produce HIGH or CRITICAL risk, got: " + critAssessment.getRiskLevel();
    }

    private static void testRationalAgentCycle() {
        Location loc = new Location("LOC-TEST-KKD", "India", "Andhra Pradesh", "Kakinada", "Kakinada", 16.9891, 82.2475, 4.0, 443000, true);
        SensorGraph graph = new SensorGraph();
        WeatherService weather = new WeatherService();
        PythonRiskClient pyClient = new PythonRiskClient(); // Tests automatic fallback

        RationalAgent agent = new RationalAgent(loc, graph, weather, pyClient);
        SensorNode rain = new SensorNode("S-TEST-RAIN-01", "Z1", loc.getLocationId(), "Rain Gauge Alpha", SensorType.RAINFALL, 16.99, 82.25, "mm", 0, 70, 120, 1.0);
        SensorNode rain2 = new SensorNode("S-TEST-RAIN-02", "Z1", loc.getLocationId(), "Rain Gauge Beta", SensorType.RAINFALL, 16.98, 82.24, "mm", 0, 70, 120, 1.0);
        SensorNode wl = new SensorNode("S-TEST-WL", "Z1", loc.getLocationId(), "Water Level Gauge", SensorType.WATER_LEVEL, 16.985, 82.245, "m", 0.5, 3.5, 4.5, 1.0);

        rain.addReading(new Reading(rain.getSensorId(), 145.0, "mm"));
        rain2.addReading(new Reading(rain2.getSensorId(), 138.0, "mm"));
        wl.addReading(new Reading(wl.getSensorId(), 4.8, "m"));

        agent.registerSensor(rain);
        agent.registerSensor(rain2);
        agent.registerSensor(wl);

        graph.addEdge(rain.getSensorId(), rain2.getSensorId(), 1.0, 0.95, "PROXIMITY");
        graph.addEdge(rain.getSensorId(), wl.getSensorId(), 0.8, 0.95, "HYDROLOGICAL");

        RationalAgent.AgentCycleReport report = agent.executeCycle();
        assert report.cycleNumber == 1 : "Cycle number should be 1";
        assert report.city.equals("Kakinada") : "City should match";
        assert report.riskScore >= 60.0 : "Confirmed critical hazard must score >= 60";
        assert report.notificationsDispatched > 0 : "Should dispatch boundary audit notifications";
        assert agent.getActiveAlerts().size() == 1 : "Should have 1 active disaster alert";
    }

    private static void testDistrictRestriction() {
        double epicLat = 16.9891;
        double epicLon = 82.2475;
        String alertDistrict = "Kakinada";

        // Case 1: User is 15km away in the SAME district -> DISASTER_ALERT sent
        double[] pt15 = HaversineUtil.calculateDestinationPoint(epicLat, epicLon, 15.0, 45.0);
        NotificationResult resSame = GeoFenceEngine.createNotificationResultWithDistrict(
                "ALT-TEST-1", "USER-SAME-DIST", "SMS", epicLat, epicLon, alertDistrict, pt15[0], pt15[1], "Kakinada"
        );
        assert resSame.getDeliveryStatus().equals("SENT") : "User in same district within 100km must receive alert";
        assert resSame.getAlertTypeAssigned().equals("DISASTER_ALERT") : "Must be DISASTER_ALERT";

        // Case 2: User is 15km away in a DIFFERENT district -> SUPPRESSED_DISTRICT_MISMATCH / FILTERED_OUT
        NotificationResult resDiff = GeoFenceEngine.createNotificationResultWithDistrict(
                "ALT-TEST-2", "USER-DIFF-DIST", "SMS", epicLat, epicLon, alertDistrict, pt15[0], pt15[1], "Visakhapatnam"
        );
        assert resDiff.getDeliveryStatus().equals("FILTERED_OUT") : "User in different district must be FILTERED_OUT";
        assert resDiff.getAlertTypeAssigned().equals("SUPPRESSED_DISTRICT_MISMATCH") : "Must be SUPPRESSED_DISTRICT_MISMATCH";
    }
}
