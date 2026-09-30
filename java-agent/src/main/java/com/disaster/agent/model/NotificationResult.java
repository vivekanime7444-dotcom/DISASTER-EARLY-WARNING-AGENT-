package com.disaster.agent.model;

import java.time.Instant;

/**
 * Audit record of notification filtering and delivery with distance reasoning.
 */
public class NotificationResult {
    private String notificationId;
    private String alertId;
    private String userId;
    private String channel;
    private double recipientLat;
    private double recipientLon;
    private double calculatedDistanceKm;
    private double qualifyingRadiusKm;
    private String alertTypeAssigned; // "DISASTER_ALERT", "WEATHER_ALERT", "SUPPRESSED_OUT_OF_RANGE"
    private String deliveryStatus;
    private String explanationReason;
    private Instant timestamp;

    public NotificationResult(String notificationId, String alertId, String userId,
                              String channel, double recipientLat, double recipientLon,
                              double calculatedDistanceKm, double qualifyingRadiusKm,
                              String alertTypeAssigned, String deliveryStatus,
                              String explanationReason, Instant timestamp) {
        this.notificationId = notificationId;
        this.alertId = alertId;
        this.userId = userId;
        this.channel = channel;
        this.recipientLat = recipientLat;
        this.recipientLon = recipientLon;
        this.calculatedDistanceKm = calculatedDistanceKm;
        this.qualifyingRadiusKm = qualifyingRadiusKm;
        this.alertTypeAssigned = alertTypeAssigned;
        this.deliveryStatus = deliveryStatus;
        this.explanationReason = explanationReason;
        this.timestamp = timestamp != null ? timestamp : Instant.now();
    }

    public String getNotificationId() { return notificationId; }
    public String getAlertId() { return alertId; }
    public String getUserId() { return userId; }
    public String getChannel() { return channel; }
    public double getRecipientLat() { return recipientLat; }
    public double getRecipientLon() { return recipientLon; }
    public double getCalculatedDistanceKm() { return calculatedDistanceKm; }
    public double getQualifyingRadiusKm() { return qualifyingRadiusKm; }
    public String getAlertTypeAssigned() { return alertTypeAssigned; }
    public String getDeliveryStatus() { return deliveryStatus; }
    public String getExplanationReason() { return explanationReason; }
    public Instant getTimestamp() { return timestamp; }
}
