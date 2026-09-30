package com.disaster.agent.geo;

import com.disaster.agent.model.Alert;
import com.disaster.agent.model.NotificationResult;

/**
 * Universal Geographic Geofencing Engine.
 * Enforces the two distinct notification radii:
 *   - DISASTER_ALERT_RADIUS = 100.0 KM
 *   - WEATHER_ALERT_RADIUS = 150.0 KM
 *
 * Rules:
 *   0 to 100.0 km    -> Disaster Alert + Weather Alert
 *   100.0+ to 150.0 km -> Weather Alert ONLY
 *   > 150.0 km       -> No location-based alert (Suppressed)
 */
public class GeoFenceEngine {

    public enum GeofenceStatus {
        DISASTER_AND_WEATHER, // 0 - 100 km within alert district
        WEATHER_ONLY,         // >100 - 150 km within alert district
        OUT_OF_RANGE,         // >150 km
        DISTRICT_MISMATCH     // Outside affected alert district
    }

    public static class GeofenceEvaluation {
        private final GeofenceStatus status;
        private final double distanceKm;
        private final String explanation;

        public GeofenceEvaluation(GeofenceStatus status, double distanceKm, String explanation) {
            this.status = status;
            this.distanceKm = distanceKm;
            this.explanation = explanation;
        }

        public GeofenceStatus getStatus() { return status; }
        public double getDistanceKm() { return distanceKm; }
        public String getExplanation() { return explanation; }
    }

    /**
     * Evaluates distance between epicenter and user, returning classification and reason.
     */
    public static GeofenceEvaluation evaluateLocation(double epicenterLat, double epicenterLon,
                                                      double userLat, double userLon) {
        double distanceKm = HaversineUtil.calculateDistanceKm(epicenterLat, epicenterLon, userLat, userLon);

        // Round to 2 decimal places to avoid floating point precision artifacts at boundaries
        double roundedDist = Math.round(distanceKm * 100.0) / 100.0;

        if (roundedDist <= Alert.DISASTER_ALERT_RADIUS_KM) { // <= 100.0 km
            return new GeofenceEvaluation(
                GeofenceStatus.DISASTER_AND_WEATHER,
                distanceKm,
                String.format("User is %.2f km from epicenter (<= 100 km threshold). Eligible for Critical Disaster Alert + Weather Alert.", distanceKm)
            );
        } else if (roundedDist <= Alert.WEATHER_ALERT_RADIUS_KM) { // 100.01 to 150.0 km
            return new GeofenceEvaluation(
                GeofenceStatus.WEATHER_ONLY,
                distanceKm,
                String.format("User is %.2f km from epicenter (> 100 km and <= 150 km threshold). Eligible for Weather Alert ONLY. Disaster warning suppressed.", distanceKm)
            );
        } else { // > 150.0 km
            return new GeofenceEvaluation(
                GeofenceStatus.OUT_OF_RANGE,
                distanceKm,
                String.format("User is %.2f km from epicenter (> 150 km threshold). Beyond alert boundary. No location-based notification dispatched.", distanceKm)
            );
        }
    }

    /**
     * Evaluates location and district.
     * Enforces: ONLY alerts sent to users within the affected alert district.
     */
    public static GeofenceEvaluation evaluateLocationWithDistrict(double epicenterLat, double epicenterLon, String alertDistrict,
                                                                  double userLat, double userLon, String userDistrict) {
        if (alertDistrict != null && userDistrict != null && !alertDistrict.isBlank() && !userDistrict.isBlank()) {
            if (!alertDistrict.trim().equalsIgnoreCase(userDistrict.trim())) {
                double distKm = HaversineUtil.calculateDistanceKm(epicenterLat, epicenterLon, userLat, userLon);
                return new GeofenceEvaluation(
                    GeofenceStatus.DISTRICT_MISMATCH,
                    distKm,
                    String.format("Alert filtered: User registered in '%s' district, but hazard is localized to '%s' district. Only sent in user alert district.",
                        userDistrict, alertDistrict)
                );
            }
        }
        return evaluateLocation(epicenterLat, epicenterLon, userLat, userLon);
    }

    /**
     * Builds an auditable NotificationResult for a specific user with district matching.
     */
    public static NotificationResult createNotificationResultWithDistrict(String alertId, String userId, String channel,
                                                                          double epicenterLat, double epicenterLon, String alertDistrict,
                                                                          double userLat, double userLon, String userDistrict) {
        GeofenceEvaluation eval = evaluateLocationWithDistrict(epicenterLat, epicenterLon, alertDistrict, userLat, userLon, userDistrict);
        String assignedAlert;
        String deliveryStatus;
        double qualifyingRadius;

        switch (eval.getStatus()) {
            case DISASTER_AND_WEATHER:
                assignedAlert = "DISASTER_ALERT";
                deliveryStatus = "SENT";
                qualifyingRadius = 100.0;
                break;
            case WEATHER_ONLY:
                assignedAlert = "WEATHER_ALERT";
                deliveryStatus = "SENT";
                qualifyingRadius = 150.0;
                break;
            case DISTRICT_MISMATCH:
                assignedAlert = "SUPPRESSED_DISTRICT_MISMATCH";
                deliveryStatus = "FILTERED_OUT";
                qualifyingRadius = 100.0;
                break;
            case OUT_OF_RANGE:
            default:
                assignedAlert = "SUPPRESSED_OUT_OF_RANGE";
                deliveryStatus = "FILTERED_OUT";
                qualifyingRadius = 150.0;
                break;
        }

        return new NotificationResult(
            "NOTIF-" + System.currentTimeMillis() + "-" + (int)(Math.random()*1000),
            alertId,
            userId,
            channel,
            userLat,
            userLon,
            eval.getDistanceKm(),
            qualifyingRadius,
            assignedAlert,
            deliveryStatus,
            eval.getExplanation(),
            java.time.Instant.now()
        );
    }

    /**
     * Builds an auditable NotificationResult without district restriction.
     */
    public static NotificationResult createNotificationResult(String alertId, String userId, String channel,
                                                              double epicenterLat, double epicenterLon,
                                                              double userLat, double userLon) {
        return createNotificationResultWithDistrict(alertId, userId, channel, epicenterLat, epicenterLon, null, userLat, userLon, null);
    }
}
