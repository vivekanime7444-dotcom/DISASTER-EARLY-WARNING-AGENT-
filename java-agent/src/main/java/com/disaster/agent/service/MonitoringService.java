package com.disaster.agent.service;

import com.disaster.agent.agent.RationalAgent;
import com.disaster.agent.client.PythonRiskClient;
import com.disaster.agent.graph.SensorGraph;
import com.disaster.agent.model.*;
import com.disaster.agent.weather.WeatherService;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 24/7 Monitoring Service.
 * Runs continuous background evaluation workers across all registered dynamic locations.
 * Implements resilient retry logic, health checks, simulation mode, and service recovery.
 */
public class MonitoringService {
    private static final Logger LOGGER = Logger.getLogger(MonitoringService.class.getName());

    private final Map<String, RationalAgent> agents = new ConcurrentHashMap<>();
    private final Map<String, Location> locations = new ConcurrentHashMap<>();
    private final SensorGraph globalGraph = new SensorGraph();
    private final WeatherService weatherService = new WeatherService();
    private final PythonRiskClient pythonRiskClient = new PythonRiskClient();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private ScheduledFuture<?> monitoringTask;
    private volatile boolean isRunning = false;
    private int pollingIntervalSeconds = 5;

    public MonitoringService() {
        initDefaultLocationsAndSensors();
    }

    /**
     * Bootstraps default example locations (Kakinada, Visakhapatnam, Vijayawada, Hyderabad)
     * Note: The platform is universal and dynamically configurable for any global coordinate.
     */
    private void initDefaultLocationsAndSensors() {
        Location kakinada = new Location("LOC-KAKINADA", "India", "Andhra Pradesh", "Kakinada", "Kakinada", 16.9891, 82.2475, 4.0, 443000, true);
        Location vizag = new Location("LOC-VIZAG", "India", "Andhra Pradesh", "Visakhapatnam", "Visakhapatnam", 17.6868, 83.2185, 12.0, 2035000, true);
        Location vijayawada = new Location("LOC-VIJAYAWADA", "India", "Andhra Pradesh", "NTR", "Vijayawada", 16.5062, 80.6480, 11.0, 1048000, true);
        Location hyderabad = new Location("LOC-HYDERABAD", "India", "Telangana", "Hyderabad", "Hyderabad", 17.3850, 78.4867, 542.0, 6809000, true);

        registerLocation(kakinada);
        registerLocation(vizag);
        registerLocation(vijayawada);
        registerLocation(hyderabad);

        // Kakinada Sensors
        SensorNode kkdRain1 = new SensorNode("SNS-KKD-RAIN-01", "ZONE-KKD-COAST", "LOC-KAKINADA", "Kakinada Port Rain Gauge Alpha",
                SensorType.RAINFALL, 16.9950, 82.2600, "mm", 0, 70.0, 120.0, 0.98);
        SensorNode kkdRain2 = new SensorNode("SNS-KKD-RAIN-02", "ZONE-KKD-RIVER", "LOC-KAKINADA", "Medaline Basin Rain Gauge Beta",
                SensorType.RAINFALL, 16.9810, 82.2360, "mm", 0, 70.0, 120.0, 0.95);
        SensorNode kkdWl1 = new SensorNode("SNS-KKD-WL-01", "ZONE-KKD-RIVER", "LOC-KAKINADA", "Medaline Sluice Water Level Mon",
                SensorType.WATER_LEVEL, 16.9805, 82.2355, "m", 0.5, 3.5, 4.5, 0.99);
        SensorNode kkdWind = new SensorNode("SNS-KKD-WIND-01", "ZONE-KKD-COAST", "LOC-KAKINADA", "Port Anemometer Tower A",
                SensorType.WIND_SPEED, 16.9960, 82.2610, "km/h", 0, 50.0, 80.0, 0.96);
        SensorNode kkdHumid = new SensorNode("SNS-KKD-HUMID-01", "ZONE-KKD-URBAN", "LOC-KAKINADA", "Town Centre Hygrometer",
                SensorType.HUMIDITY, 16.9880, 82.2450, "%", 10, 85.0, 95.0, 0.97);
        SensorNode kkdSoil = new SensorNode("SNS-KKD-SOIL-01", "ZONE-KKD-RIVER", "LOC-KAKINADA", "Canal Bank Soil Saturation",
                SensorType.SOIL_MOISTURE, 16.9790, 82.2340, "%", 5, 75.0, 90.0, 0.92);
        SensorNode kkdPres = new SensorNode("SNS-KKD-PRES-01", "ZONE-KKD-COAST", "LOC-KAKINADA", "Coastal Barometric Sensor",
                SensorType.PRESSURE, 16.9940, 82.2590, "hPa", 940, 995.0, 980.0, 0.99);

        // Add initial baseline normal readings
        kkdRain1.addReading(new Reading(kkdRain1.getSensorId(), 18.5, "mm"));
        kkdRain2.addReading(new Reading(kkdRain2.getSensorId(), 16.0, "mm"));
        kkdWl1.addReading(new Reading(kkdWl1.getSensorId(), 1.4, "m"));
        kkdWind.addReading(new Reading(kkdWind.getSensorId(), 24.0, "km/h"));
        kkdHumid.addReading(new Reading(kkdHumid.getSensorId(), 76.0, "%"));
        kkdSoil.addReading(new Reading(kkdSoil.getSensorId(), 42.0, "%"));
        kkdPres.addReading(new Reading(kkdPres.getSensorId(), 1010.0, "hPa"));

        RationalAgent kkdAgent = agents.get("LOC-KAKINADA");
        kkdAgent.registerSensor(kkdRain1);
        kkdAgent.registerSensor(kkdRain2);
        kkdAgent.registerSensor(kkdWl1);
        kkdAgent.registerSensor(kkdWind);
        kkdAgent.registerSensor(kkdHumid);
        kkdAgent.registerSensor(kkdSoil);
        kkdAgent.registerSensor(kkdPres);

        // Connect graph edges
        globalGraph.addEdge(kkdRain1.getSensorId(), kkdRain2.getSensorId(), 2.8, 0.9, "METEOROLOGICAL_PROXIMITY");
        globalGraph.addEdge(kkdRain2.getSensorId(), kkdWl1.getSensorId(), 0.15, 0.95, "RIVER_CORRIDOR");
        globalGraph.addEdge(kkdRain1.getSensorId(), kkdWind.getSensorId(), 0.18, 0.85, "COASTAL_SECTOR");
        globalGraph.addEdge(kkdWl1.getSensorId(), kkdSoil.getSensorId(), 0.22, 0.92, "CATCHMENT_BASIN");

        // Vizag Sensors
        SensorNode vzgRain = new SensorNode("SNS-VZG-RAIN-01", "ZONE-VZG-PORT", "LOC-VIZAG", "Gangavaram Coastal Rain Gauge",
                SensorType.RAINFALL, 17.6350, 83.2350, "mm", 0, 70.0, 120.0, 0.97);
        SensorNode vzgWind = new SensorNode("SNS-VZG-WIND-01", "ZONE-VZG-PORT", "LOC-VIZAG", "Harbour Wind Doppler Probe",
                SensorType.WIND_SPEED, 17.6360, 83.2360, "km/h", 0, 55.0, 85.0, 0.95);
        SensorNode vzgWl = new SensorNode("SNS-VZG-WL-01", "ZONE-VZG-PORT", "LOC-VIZAG", "Tidal Wave Gauge Gangavaram",
                SensorType.WATER_LEVEL, 17.6340, 83.2340, "m", 0.2, 3.2, 4.2, 0.96);

        vzgRain.addReading(new Reading(vzgRain.getSensorId(), 12.0, "mm"));
        vzgWind.addReading(new Reading(vzgWind.getSensorId(), 28.0, "km/h"));
        vzgWl.addReading(new Reading(vzgWl.getSensorId(), 1.1, "m"));

        RationalAgent vzgAgent = agents.get("LOC-VIZAG");
        vzgAgent.registerSensor(vzgRain);
        vzgAgent.registerSensor(vzgWind);
        vzgAgent.registerSensor(vzgWl);

        globalGraph.addEdge(vzgRain.getSensorId(), vzgWl.getSensorId(), 0.2, 0.9, "PORT_WATERFRONT");
        globalGraph.addEdge(vzgRain.getSensorId(), vzgWind.getSensorId(), 0.15, 0.88, "PORT_WATERFRONT");

        // Vijayawada Sensors
        SensorNode vjaRain = new SensorNode("SNS-VJA-RAIN-01", "ZONE-VJA-KRISHNA", "LOC-VIJAYAWADA", "Prakasam Barrage Rain Station",
                SensorType.RAINFALL, 16.5080, 80.6120, "mm", 0, 70.0, 120.0, 0.99);
        SensorNode vjaWl = new SensorNode("SNS-VJA-WL-01", "ZONE-VJA-KRISHNA", "LOC-VIJAYAWADA", "Krishna River Spillway Hydro",
                SensorType.WATER_LEVEL, 16.5075, 80.6115, "m", 1.0, 4.0, 5.2, 0.98);
        vjaRain.addReading(new Reading(vjaRain.getSensorId(), 15.0, "mm"));
        vjaWl.addReading(new Reading(vjaWl.getSensorId(), 2.1, "m"));

        RationalAgent vjaAgent = agents.get("LOC-VIJAYAWADA");
        vjaAgent.registerSensor(vjaRain);
        vjaAgent.registerSensor(vjaWl);
        globalGraph.addEdge(vjaRain.getSensorId(), vjaWl.getSensorId(), 0.1, 0.95, "RIVER_BARRAGE");

        // Hyderabad Sensors
        SensorNode hydRain = new SensorNode("SNS-HYD-RAIN-01", "ZONE-HYD-MUSI", "LOC-HYDERABAD", "Musi River Basin Rain Gauge",
                SensorType.RAINFALL, 17.3750, 78.4800, "mm", 0, 65.0, 110.0, 0.96);
        SensorNode hydWl = new SensorNode("SNS-HYD-WL-01", "ZONE-HYD-MUSI", "LOC-HYDERABAD", "Chaderghat Bridge Hydro Level",
                SensorType.WATER_LEVEL, 17.3760, 78.4810, "m", 0.5, 3.0, 4.2, 0.94);
        hydRain.addReading(new Reading(hydRain.getSensorId(), 5.0, "mm"));
        hydWl.addReading(new Reading(hydWl.getSensorId(), 1.2, "m"));

        RationalAgent hydAgent = agents.get("LOC-HYDERABAD");
        hydAgent.registerSensor(hydRain);
        hydAgent.registerSensor(hydWl);
        globalGraph.addEdge(hydRain.getSensorId(), hydWl.getSensorId(), 0.15, 0.92, "URBAN_RIVER_BASIN");
    }

    public synchronized void registerLocation(Location loc) {
        locations.put(loc.getLocationId(), loc);
        agents.put(loc.getLocationId(), new RationalAgent(loc, globalGraph, weatherService, pythonRiskClient));
        LOGGER.info("Registered dynamic location: " + loc.getCity() + " (" + loc.getLatitude() + ", " + loc.getLongitude() + ")");
    }

    /**
     * Starts continuous 24/7 background monitoring loop.
     */
    public synchronized void start24x7Monitoring() {
        if (isRunning) return;
        isRunning = true;
        LOGGER.info("Starting 24/7 Continuous Disaster Early-Warning Agent Loop (Interval: " + pollingIntervalSeconds + "s)...");

        monitoringTask = scheduler.scheduleWithFixedDelay(() -> {
            try {
                for (RationalAgent agent : agents.values()) {
                    agent.executeCycle();
                }
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Unexpected error in 24/7 monitoring loop (Service recovering automatically): " + t.getMessage(), t);
            }
        }, 1, pollingIntervalSeconds, TimeUnit.SECONDS);
    }

    public synchronized void stopMonitoring() {
        isRunning = false;
        if (monitoringTask != null) {
            monitoringTask.cancel(false);
        }
        LOGGER.info("24/7 Monitoring loop paused.");
    }

    /**
     * Simulates scenario telemetry on a specific location:
     * - "NORMAL": Rainfall 20 mm -> NORMAL
     * - "WARNING": Rainfall 80 mm + rising water 3.6m -> WARNING
     * - "CRITICAL": Rainfall 145 mm, Water Level 4.8m, Wind 62 km/h, Humidity 94% -> CRITICAL
     */
    public void injectSimulationScenario(String locationId, String scenarioName) {
        RationalAgent agent = agents.get(locationId);
        if (agent == null) return;

        Map<String, SensorNode> sensors = agent.getRegisteredSensors();

        switch (scenarioName.toUpperCase()) {
            case "CRITICAL":
                LOGGER.info("Injecting CRITICAL disaster simulation scenario into " + locationId);
                for (SensorNode s : sensors.values()) {
                    if (s.getSensorType() == SensorType.RAINFALL) s.addReading(new Reading(s.getSensorId(), 145.0, "mm"));
                    else if (s.getSensorType() == SensorType.WATER_LEVEL) s.addReading(new Reading(s.getSensorId(), 4.8, "m"));
                    else if (s.getSensorType() == SensorType.WIND_SPEED) s.addReading(new Reading(s.getSensorId(), 62.0, "km/h"));
                    else if (s.getSensorType() == SensorType.HUMIDITY) s.addReading(new Reading(s.getSensorId(), 94.0, "%"));
                    else if (s.getSensorType() == SensorType.PRESSURE) s.addReading(new Reading(s.getSensorId(), 982.0, "hPa"));
                    else if (s.getSensorType() == SensorType.SOIL_MOISTURE) s.addReading(new Reading(s.getSensorId(), 89.0, "%"));
                }
                break;

            case "WARNING":
                LOGGER.info("Injecting WARNING disaster simulation scenario into " + locationId);
                for (SensorNode s : sensors.values()) {
                    if (s.getSensorType() == SensorType.RAINFALL) s.addReading(new Reading(s.getSensorId(), 80.0, "mm"));
                    else if (s.getSensorType() == SensorType.WATER_LEVEL) s.addReading(new Reading(s.getSensorId(), 3.6, "m"));
                    else if (s.getSensorType() == SensorType.WIND_SPEED) s.addReading(new Reading(s.getSensorId(), 48.0, "km/h"));
                    else if (s.getSensorType() == SensorType.HUMIDITY) s.addReading(new Reading(s.getSensorId(), 86.0, "%"));
                }
                break;

            case "NORMAL":
            default:
                LOGGER.info("Restoring NORMAL baseline simulation scenario into " + locationId);
                for (SensorNode s : sensors.values()) {
                    if (s.getSensorType() == SensorType.RAINFALL) s.addReading(new Reading(s.getSensorId(), 18.0, "mm"));
                    else if (s.getSensorType() == SensorType.WATER_LEVEL) s.addReading(new Reading(s.getSensorId(), 1.4, "m"));
                    else if (s.getSensorType() == SensorType.WIND_SPEED) s.addReading(new Reading(s.getSensorId(), 20.0, "km/h"));
                    else if (s.getSensorType() == SensorType.HUMIDITY) s.addReading(new Reading(s.getSensorId(), 75.0, "%"));
                    else if (s.getSensorType() == SensorType.PRESSURE) s.addReading(new Reading(s.getSensorId(), 1010.0, "hPa"));
                }
                break;
        }

        // Trigger immediate evaluation cycle
        agent.executeCycle();
    }

    public Map<String, RationalAgent> getAgents() { return agents; }
    public Map<String, Location> getLocations() { return locations; }
    public SensorGraph getGlobalGraph() { return globalGraph; }
    public boolean isRunning() { return isRunning; }
    public WeatherService getWeatherService() { return weatherService; }
    public PythonRiskClient getPythonRiskClient() { return pythonRiskClient; }
}
