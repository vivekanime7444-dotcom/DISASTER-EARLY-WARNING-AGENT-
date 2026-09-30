package com.disaster.agent.geo;

/**
 * High-precision spherical distance calculation using the Haversine formula.
 * Earth mean radius: 6371.0088 kilometers.
 */
public class HaversineUtil {
    public static final double EARTH_RADIUS_KM = 6371.0088;

    /**
     * Calculates great-circle distance between two geographic coordinates in kilometers.
     */
    public static double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double originLat = Math.toRadians(lat1);
        double targetLat = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.sin(dLon / 2) * Math.sin(dLon / 2) *
                   Math.cos(originLat) * Math.cos(targetLat);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_KM * c;
    }

    /**
     * Helper to compute a destination point given starting point, distance (km), and bearing (degrees).
     * Used for creating exact boundary test points at 100.0km, 100.1km, 150.0km, 150.1km.
     */
    public static double[] calculateDestinationPoint(double lat, double lon, double distanceKm, double bearingDeg) {
        double distRatio = distanceKm / EARTH_RADIUS_KM;
        double bearingRad = Math.toRadians(bearingDeg);
        double latRad = Math.toRadians(lat);
        double lonRad = Math.toRadians(lon);

        double targetLatRad = Math.asin(Math.sin(latRad) * Math.cos(distRatio) +
                Math.cos(latRad) * Math.sin(distRatio) * Math.cos(bearingRad));

        double targetLonRad = lonRad + Math.atan2(Math.sin(bearingRad) * Math.sin(distRatio) * Math.cos(latRad),
                Math.cos(distRatio) - Math.sin(latRad) * Math.sin(targetLatRad));

        return new double[]{Math.toDegrees(targetLatRad), Math.toDegrees(targetLonRad)};
    }
}
