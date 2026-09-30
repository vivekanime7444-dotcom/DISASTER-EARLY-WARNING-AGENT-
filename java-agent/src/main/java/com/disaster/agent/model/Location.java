package com.disaster.agent.model;

/**
 * Dynamic location entity:
 * Country -> State -> District -> City/Town -> Zone
 * Works for any geographic coordinates worldwide.
 */
public class Location {
    private String locationId;
    private String country;
    private String state;
    private String district;
    private String city;
    private double latitude;
    private double longitude;
    private double elevationMeters;
    private int population;
    private boolean active;

    public Location(String locationId, String country, String state, String district,
                    String city, double latitude, double longitude, double elevationMeters,
                    int population, boolean active) {
        this.locationId = locationId;
        this.country = country != null ? country : "India";
        this.state = state;
        this.district = district;
        this.city = city;
        this.latitude = latitude;
        this.longitude = longitude;
        this.elevationMeters = elevationMeters;
        this.population = population;
        this.active = active;
    }

    public String getLocationId() { return locationId; }
    public String getCountry() { return country; }
    public String getState() { return state; }
    public String getDistrict() { return district; }
    public String getCity() { return city; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public double getElevationMeters() { return elevationMeters; }
    public int getPopulation() { return population; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getFullHierarchy() {
        return country + " > " + state + " > " + district + " > " + city;
    }
}
