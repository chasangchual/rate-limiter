package com.fortitude.resilience.circuitbreaker;

public enum CircuitBreakerState {
    CLOSE,
    OPEN,
    HALF_OPEN
}
