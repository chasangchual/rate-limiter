package com.fortitude.circuitbreaker;

import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class CircuitBreakerTest {

    @Test
    void run() {
        Random random = new Random();

        DownstreamCaller caller = new DownstreamCaller();
        CircuitBreaker circuitBreaker = new CircuitBreaker();
        IntStream.range(0, 1000).forEach(i -> {
            try {
                circuitBreaker.run(caller);
            } catch (Exception e) {
                System.out.println("Exception occurred : " + e.getMessage());
            }
            long betweenCalls = (long) (random.nextDouble() * 300);
            if(circuitBreaker.getState().equals(CircuitBreakerState.OPEN)) {
                try {
                    Thread.sleep(200 + betweenCalls);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }
}