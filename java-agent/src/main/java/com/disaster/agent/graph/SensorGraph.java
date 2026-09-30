package com.disaster.agent.graph;

import com.disaster.agent.model.*;
import java.util.*;

/**
 * ADSA (Advanced Data Structures & Algorithms) Sensor Network Graph.
 * Implements Adjacency Lists, BFS, DFS, Weighted Connections, Connected Components,
 * Dijkstra, and Spatial Neighbor-Correlation Anomaly Detection.
 */
public class SensorGraph {
    private final Map<String, SensorNode> nodes = new HashMap<>();
    private final Map<String, List<SensorEdge>> adjList = new HashMap<>();

    public synchronized void addNode(SensorNode node) {
        nodes.put(node.getSensorId(), node);
        adjList.putIfAbsent(node.getSensorId(), new ArrayList<>());
    }

    public synchronized void addEdge(String srcId, String tgtId, double distanceKm, double weight, String couplingType) {
        if (!nodes.containsKey(srcId) || !nodes.containsKey(tgtId)) return;
        
        adjList.computeIfAbsent(srcId, k -> new ArrayList<>())
               .add(new SensorEdge(srcId, tgtId, distanceKm, weight, couplingType));
        
        // Undirected graph by default for physical sensor proximity
        adjList.computeIfAbsent(tgtId, k -> new ArrayList<>())
               .add(new SensorEdge(tgtId, srcId, distanceKm, weight, couplingType));
    }

    public SensorNode getNode(String sensorId) {
        return nodes.get(sensorId);
    }

    public Collection<SensorNode> getAllNodes() {
        return nodes.values();
    }

    public List<SensorEdge> getEdges(String sensorId) {
        return adjList.getOrDefault(sensorId, Collections.emptyList());
    }

    /**
     * ADSA BFS: Breadth-First Search traversal starting from a given sensor.
     * Useful for visualizing flood propagation waves across sensor zones.
     */
    public List<String> bfs(String startSensorId) {
        List<String> traversal = new ArrayList<>();
        if (!nodes.containsKey(startSensorId)) return traversal;

        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        visited.add(startSensorId);
        queue.add(startSensorId);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            traversal.add(current);

            for (SensorEdge edge : adjList.getOrDefault(current, Collections.emptyList())) {
                String neighbor = edge.getTargetSensorId();
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return traversal;
    }

    /**
     * ADSA DFS: Depth-First Search traversal for exploring deep river catchment chains.
     */
    public List<String> dfs(String startSensorId) {
        List<String> traversal = new ArrayList<>();
        if (!nodes.containsKey(startSensorId)) return traversal;

        Set<String> visited = new HashSet<>();
        dfsRecursive(startSensorId, visited, traversal);
        return traversal;
    }

    private void dfsRecursive(String current, Set<String> visited, List<String> traversal) {
        visited.add(current);
        traversal.add(current);

        for (SensorEdge edge : adjList.getOrDefault(current, Collections.emptyList())) {
            String neighbor = edge.getTargetSensorId();
            if (!visited.contains(neighbor)) {
                dfsRecursive(neighbor, visited, traversal);
            }
        }
    }

    /**
     * ADSA Connected Components: Detects isolated subgraphs / geographic cluster clusters.
     */
    public List<Set<String>> getConnectedComponents() {
        List<Set<String>> components = new ArrayList<>();
        Set<String> visited = new HashSet<>();

        for (String sensorId : nodes.keySet()) {
            if (!visited.contains(sensorId)) {
                Set<String> component = new HashSet<>();
                Queue<String> queue = new LinkedList<>();
                
                visited.add(sensorId);
                queue.add(sensorId);

                while (!queue.isEmpty()) {
                    String curr = queue.poll();
                    component.add(curr);
                    for (SensorEdge edge : adjList.getOrDefault(curr, Collections.emptyList())) {
                        String neighbor = edge.getTargetSensorId();
                        if (!visited.contains(neighbor)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
                components.add(component);
            }
        }
        return components;
    }

    /**
     * ADSA Dijkstra: Shortest path based on inverted correlation weight (least resistance / fastest propagation).
     */
    public List<String> findShortestWeightedPath(String startId, String endId) {
        if (!nodes.containsKey(startId) || !nodes.containsKey(endId)) return Collections.emptyList();

        Map<String, Double> distances = new HashMap<>();
        Map<String, String> previous = new HashMap<>();
        PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingDouble(nd -> nd.distance));

        for (String id : nodes.keySet()) {
            distances.put(id, Double.MAX_VALUE);
        }
        distances.put(startId, 0.0);
        pq.add(new NodeDistance(startId, 0.0));

        while (!pq.isEmpty()) {
            NodeDistance curr = pq.poll();
            if (curr.sensorId.equals(endId)) break;
            if (curr.distance > distances.get(curr.sensorId)) continue;

            for (SensorEdge edge : adjList.getOrDefault(curr.sensorId, Collections.emptyList())) {
                double edgeCost = (1.0 / edge.getWeight()) + (edge.getDistanceKm() * 0.1);
                double newDist = curr.distance + edgeCost;
                if (newDist < distances.get(edge.getTargetSensorId())) {
                    distances.put(edge.getTargetSensorId(), newDist);
                    previous.put(edge.getTargetSensorId(), curr.sensorId);
                    pq.add(new NodeDistance(edge.getTargetSensorId(), newDist));
                }
            }
        }

        List<String> path = new ArrayList<>();
        String step = endId;
        if (!previous.containsKey(step) && !startId.equals(endId)) return path;

        while (step != null) {
            path.add(0, step);
            step = previous.get(step);
        }
        return path;
    }

    /**
     * ADSA Spatial Neighbor Correlation:
     * Cross-verifies abnormal sensor readings with neighboring nodes.
     * Prevents false alarms from isolated sensor anomalies and confirms genuine disaster trends.
     */
    public NeighborCorrelationResult validateWithNeighbors(String sensorId, Reading reading) {
        SensorNode target = nodes.get(sensorId);
        if (target == null) {
            return new NeighborCorrelationResult(false, 0, 0, "Sensor not found in graph", 0.5);
        }

        List<SensorEdge> edges = adjList.getOrDefault(sensorId, Collections.emptyList());
        if (edges.isEmpty()) {
            return new NeighborCorrelationResult(true, 0, 0, "Isolated node: No graph neighbors for cross-validation", 0.7);
        }

        int totalNeighbors = 0;
        int confirmingCount = 0;
        List<String> confirmingSensors = new ArrayList<>();

        for (SensorEdge edge : edges) {
            SensorNode neighborNode = nodes.get(edge.getTargetSensorId());
            if (neighborNode == null || neighborNode.getHealthStatus() == HealthStatus.OFFLINE) continue;

            Reading neighborReading = neighborNode.getLatestReading();
            if (neighborReading == null) continue;

            totalNeighbors++;

            // Physical Cross-Check Logic:
            // 1. Same sensor type (e.g. adjacent rain gauges)
            if (neighborNode.getSensorType() == target.getSensorType()) {
                if (neighborReading.getValue() >= neighborNode.getWarningThreshold() * 0.8) {
                    confirmingCount++;
                    confirmingSensors.add(neighborNode.getName() + " (" + neighborReading.getValue() + " " + neighborNode.getUnit() + ")");
                }
            }
            // 2. Hydrological coupling: Rainfall spike confirmed by Rising Water Level
            else if (target.getSensorType() == SensorType.RAINFALL && neighborNode.getSensorType() == SensorType.WATER_LEVEL) {
                if (neighborReading.getValue() >= neighborNode.getWarningThreshold() * 0.75) {
                    confirmingCount++;
                    confirmingSensors.add(neighborNode.getName() + " [Water Level: " + neighborReading.getValue() + "m]");
                }
            }
            // 3. Meteorological coupling: Barometric Pressure drop confirmed by Wind Speed surge
            else if (target.getSensorType() == SensorType.PRESSURE && neighborNode.getSensorType() == SensorType.WIND_SPEED) {
                if (neighborReading.getValue() >= neighborNode.getWarningThreshold() * 0.75) {
                    confirmingCount++;
                    confirmingSensors.add(neighborNode.getName() + " [Wind Surge: " + neighborReading.getValue() + "km/h]");
                }
            }
        }

        boolean isConfirmed = confirmingCount > 0;
        double confidenceWeight;
        String explanation;

        if (isConfirmed) {
            confidenceWeight = 0.95;
            explanation = String.format("Cluster confirmation: %d of %d neighboring sensors confirm abnormal readings (%s)",
                    confirmingCount, totalNeighbors, String.join(", ", confirmingSensors));
        } else if (totalNeighbors == 0) {
            confidenceWeight = 0.65;
            explanation = "No active neighbors available for cross-validation; relying on historical baseline.";
        } else {
            confidenceWeight = 0.35;
            explanation = String.format("Possible isolated anomaly: 0 of %d neighboring sensors confirm elevation. May be localized spike or hardware artifact.", totalNeighbors);
        }

        return new NeighborCorrelationResult(isConfirmed, confirmingCount, totalNeighbors, explanation, confidenceWeight);
    }

    private static class NodeDistance {
        String sensorId;
        double distance;
        NodeDistance(String id, double dist) {
            this.sensorId = id;
            this.distance = dist;
        }
    }

    public static class NeighborCorrelationResult {
        private final boolean supportedByNeighbors;
        private final int confirmingCount;
        private final int totalNeighbors;
        private final String explanation;
        private final double confidenceWeight;

        public NeighborCorrelationResult(boolean supportedByNeighbors, int confirmingCount,
                                        int totalNeighbors, String explanation, double confidenceWeight) {
            this.supportedByNeighbors = supportedByNeighbors;
            this.confirmingCount = confirmingCount;
            this.totalNeighbors = totalNeighbors;
            this.explanation = explanation;
            this.confidenceWeight = confidenceWeight;
        }

        public boolean isSupportedByNeighbors() { return supportedByNeighbors; }
        public int getConfirmingCount() { return confirmingCount; }
        public int getTotalNeighbors() { return totalNeighbors; }
        public String getExplanation() { return explanation; }
        public double getConfidenceWeight() { return confidenceWeight; }
    }
}
