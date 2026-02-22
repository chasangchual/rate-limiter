package com.fortitude.circuitbreaker;

public class CircuitCallWindow {
    private final boolean[] failWindow;
    private final boolean[] slowWindow;
    private int failCount = 0;
    private int slowCount = 0;
    private int windowSize = 0;
    private int total = 0;
    private final int MIN_WINDOWS_SIZE = 10;

    public CircuitCallWindow(int size) {
        if(size < MIN_WINDOWS_SIZE) {
            throw new RuntimeException("windows size should be equal to or greater than " + MIN_WINDOWS_SIZE);
        }
        windowSize = size;
        failWindow = new boolean[size];
        slowWindow = new boolean[size];
    }

    public synchronized void add(Boolean isFail, Boolean isSlow) {
        int index = total % windowSize;

        if(total > windowSize) {
            failCount = failWindow[index] ? failCount - 1 : failCount;
            slowCount = slowWindow[index] ? slowCount - 1 : slowCount;
        }

        failWindow[index] = isFail;
        slowWindow[index] = isSlow;

        failCount = isFail ? failCount + 1 : failCount;
        slowCount = isSlow ? slowCount + 1 : slowCount;

        total++;
    }

    public CircuitWindowSnapshot captureSnapshot() {
        return new CircuitWindowSnapshot(windowSize, failCount, slowCount, total);
    }
}
