package com.disaster.agent.model;

/**
 * Micro-location zone subdivision (e.g., River Basin, Coastal Sector, Hill Slope).
 */
public class Zone {
    private String zoneId;
    private String locationId;
    private String zoneName;
    private String zoneType;
    private double latitude;
    private double longitude;
    private double radiusKm;
    private double vulnerabilityIndex; // 0.0 to 1.0

    public Zone(String zoneId, String locationId, String zoneName, String zoneType,
                double latitude, double longitude, double radiusKm, double vulnerabilityIndex) {
        this.zoneId = zoneId;
        this.locationId = locationId;
        this.zoneName = zoneName;
        this.zoneType = zoneType;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusKm = radiusKm;
        this.vulnerabilityIndex = vulnerabilityIndex;
    }

    public String getZoneId() { return zoneId; }
    public String getLocationId() { return locationId; }
    public String getZoneName() { return zoneName; }
    public String getZoneType() { return zoneType; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getRadiusKm() { return radiusKm; }
    public double getVulnerabilityIndex() { return vulnerabilityIndex; }
}
