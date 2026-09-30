package com.disaster.agent.model;

import java.time.Instant;

/**
 * Weather intelligence model capturing real-time meteorological conditions and forecasts.
 */
public class WeatherData {
    private String locationId;
    private String provider;
    private double temperatureC;
    private double humidityPct;
    private double pressureHpa;
    private double windSpeedKmh;
    private double windDirectionDeg;
    private double rainfallMm;
    private String forecastSummary;
    private String severeWarning;
    private boolean isStale;
    private Instant recordedAt;

    public WeatherData(String locationId, String provider, double temperatureC,
                       double humidityPct, double pressureHpa, double windSpeedKmh,
                       double windDirectionDeg, double rainfallMm,
                       String forecastSummary, String severeWarning,
                       boolean isStale, Instant recordedAt) {
        this.locationId = locationId;
        this.provider = provider;
        this.temperatureC = temperatureC;
        this.humidityPct = humidityPct;
        this.pressureHpa = pressureHpa;
        this.windSpeedKmh = windSpeedKmh;
        this.windDirectionDeg = windDirectionDeg;
        this.rainfallMm = rainfallMm;
        this.forecastSummary = forecastSummary;
        this.severeWarning = severeWarning;
        this.isStale = isStale;
        this.recordedAt = recordedAt != null ? recordedAt : Instant.now();
    }

    public String getLocationId() { return locationId; }
    public String getProvider() { return provider; }
    public double getTemperatureC() { return temperatureC; }
    public double getHumidityPct() { return humidityPct; }
    public double getPressureHpa() { return pressureHpa; }
    public double getWindSpeedKmh() { return windSpeedKmh; }
    public double getWindDirectionDeg() { return windDirectionDeg; }
    public double getRainfallMm() { return rainfallMm; }
    public String getForecastSummary() { return forecastSummary; }
    public String getSevereWarning() { return severeWarning; }
    public boolean isStale() { return isStale; }
    public void setStale(boolean stale) { isStale = stale; }
    public Instant getRecordedAt() { return recordedAt; }
}
