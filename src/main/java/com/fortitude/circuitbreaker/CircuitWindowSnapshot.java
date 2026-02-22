package com.fortitude.circuitbreaker;

public record CircuitWindowSnapshot(int windowSize, int failures, int slows, int totalCalls) {
    public double getFailRate() {
        return (double) failures / windowSize;
    }

    public double getSlowRate() {
        return (double) slows / windowSize;
    }
}
