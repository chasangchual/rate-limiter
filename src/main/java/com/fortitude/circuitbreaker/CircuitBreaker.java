package com.fortitude.circuitbreaker;

import java.math.BigDecimal;
import java.util.concurrent.Callable;

/**
 * Simple implementation of a Circuit Breaker pattern.
 *
 * This class monitors downstream call results using a fixed-size sliding window.
 * It transitions between CLOSE, OPEN, and HALF_OPEN states based on:
 *  - Failure rate
 *  - Slow call rate
 *
 * CLOSE      : All calls are allowed. Metrics are collected.
 * OPEN       : Calls are rejected immediately (fast-fail).
 * HALF_OPEN  : Limited calls are allowed to test downstream recovery.
 *
 * Time is measured using System.nanoTime().
 */
public class CircuitBreaker {
    private final int DEFAULT_WINDOW_SIZE = 15;
    public final BigDecimal CIRCUIT_OPEN_THRESHOLD_BY_FAIL = BigDecimal.valueOf(0.2);
    public final BigDecimal CIRCUIT_OPEN_THRESHOLD_BY_SLOW = BigDecimal.valueOf(0.4);

    public final long MAX_SLOW = 300 * 1000 * 1000; // 300ms

    public final long HALF_OPEN_LOOK_UP = 1 * 1000 * 1000 * 1000; // 1 second (milli -> micro -> nano)
    public final long KEEP_OPEN_STATE = 1 * 1000 * 1000 * 1000; // 1 second (milli -> micro -> nano)
    public CircuitBreakerState state = CircuitBreakerState.CLOSE;
    private long circuitOpenAt = 0L;

    private long circuitHalfOpenAt = 0 ;
    private int totalSuccessInHalfOpen = 0 ;
    public final int MAX_TRIES_IN_HALF_OPEN =10;

    CircuitCallWindow circuitCallWindow = new CircuitCallWindow(DEFAULT_WINDOW_SIZE);

    /**
     * Executes the given callable within the protection of the Circuit Breaker.
     *
     * Behavior:
     *  - If circuit is OPEN, fail fast.
     *  - Otherwise execute the downstream call.
     *  - Record success/failure and slow-call metrics.
     *
     * @param callable downstream operation
     * @return result of downstream call
     * @throws Exception if downstream throws or circuit is open
     */
    public <T> T run(Callable<T> callable) throws Exception {
        // fast fail
        if (isOpen()) {
            throw new RuntimeException("Fast-fail because Circuit is open");
        }

        Boolean isSuccess = false;
        long startedAt = System.nanoTime();
        try {
            T result = callable.call();
            isSuccess = true;
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            long completedAt = System.nanoTime();
            long executionTimeInNano = completedAt - startedAt;

            boolean isSlow = executionTimeInNano > MAX_SLOW;
            handle(isSuccess, isSlow);
        }
    }

    private Boolean isOpen() {
        return !isClosed();
    }

    /**
     * Handles state transition logic after each call.
     *
     * This method:
     *  - Updates sliding window metrics
     *  - Evaluates failure and slow-call rates
     *  - Performs state transitions between CLOSE, OPEN, and HALF_OPEN
     *
     * Synchronized to protect state mutation.
     */
    private synchronized void handle(boolean isSuccess, boolean isSlow) {
        circuitCallWindow.add(!isSuccess, isSlow);
        var snapshot = circuitCallWindow.captureSnapshot();

        // run until it fills the windows
        if(snapshot.totalCalls() <= snapshot.windowSize()) {
            return;
        }

        long now = System.nanoTime();

        System.out.println("[CircuitBreaker] Current state: " + this.state.name());

        if(CircuitBreakerState.CLOSE.equals(this.state)) {
            System.out.println("[CircuitBreaker] Evaluating metrics → failureRate="
                    + snapshot.getFailRate() + ", slowRate=" + snapshot.getSlowRate());
            // open the circuit, for the too many failures
            if (CIRCUIT_OPEN_THRESHOLD_BY_FAIL.compareTo(BigDecimal.valueOf(snapshot.getFailRate())) > 0 ||
                    CIRCUIT_OPEN_THRESHOLD_BY_SLOW.compareTo(BigDecimal.valueOf(snapshot.getSlowRate())) > 0) {
                circuitOpenAt = System.nanoTime();
                this.state = CircuitBreakerState.OPEN;
                this.circuitOpenAt = now;
                System.out.println("[CircuitBreaker] State transition: CLOSE → OPEN (threshold exceeded)");
            } else {
                this.state = CircuitBreakerState.CLOSE;
            }

            return;
        }

        if(CircuitBreakerState.OPEN.equals(this.state)) {
            // if the attempt after OPEN is success
            if(isSuccess && !isSlow) {
                this.state = CircuitBreakerState.HALF_OPEN;
                totalSuccessInHalfOpen = 0;
                circuitHalfOpenAt = now;
                System.out.println("[CircuitBreaker] State transition: OPEN → HALF_OPEN (cooldown expired, testing recovery)");
            } else {
                this.state = CircuitBreakerState.OPEN;
            }
            return;
        }

        if(CircuitBreakerState.HALF_OPEN.equals(this.state)) {
            if(isSuccess && !isSlow) {
                totalSuccessInHalfOpen++;
                // keep success while HALF_OPEN state
                if((now - circuitHalfOpenAt) > HALF_OPEN_LOOK_UP) {
                    this.state = CircuitBreakerState.CLOSE;
                    System.out.println("[CircuitBreaker] State transition: HALF_OPEN → CLOSE (recovery confirmed)");
                } else {
                    this.state = CircuitBreakerState.HALF_OPEN;
                }
            } else {
                // open the circuit for any failures if it is in HALF_OPEN state
                this.state = CircuitBreakerState.OPEN;
                this.circuitOpenAt = now;
                System.out.println("[CircuitBreaker] State transition: HALF_OPEN → OPEN (test call failed)");
            }
            return;
        }

    }

    /**
     * Determines whether the circuit should allow execution.
     *
     * Logic:
     *  - If OPEN and cooldown period has not elapsed → reject.
     *  - If OPEN and cooldown elapsed → transition to HALF_OPEN.
     *  - If HALF_OPEN → allow limited number of test calls.
     *  - If CLOSE → allow all calls.
     *
     * @return true if execution is permitted
     */
    private synchronized boolean isClosed() {
        long now = System.nanoTime();

        if(CircuitBreakerState.OPEN.equals(this.state)) {
            if((now - circuitOpenAt) > KEEP_OPEN_STATE) {
                circuitHalfOpenAt = now;
                this.state = CircuitBreakerState.HALF_OPEN;
                return true;
            }
            return false;
        }

        if(CircuitBreakerState.HALF_OPEN.equals(this.state)) {
            // allow limited calls during HALF_OPEN period
            if(totalSuccessInHalfOpen <= MAX_TRIES_IN_HALF_OPEN) {
                return true;
            } else {
                return false;
            }
        }

        return true;
    }

    public CircuitBreakerState getState() {
        return this.state;
    }
}
