package com.disaster.agent.model;

public enum ValidationStatus {
    VALID,
    OUT_OF_RANGE,
    STALE,
    SUDDEN_SPIKE,
    INCONSISTENT_WITH_NEIGHBORS
}
