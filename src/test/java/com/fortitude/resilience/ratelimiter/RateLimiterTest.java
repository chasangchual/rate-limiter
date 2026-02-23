package com.fortitude.resilience.ratelimiter;

import com.fortitude.resilience.DownstreamCaller;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void acquire() {
        Random random = new Random();

        String API_ID = RateLimiter.API_1;

        DownstreamCaller caller = new DownstreamCaller();
        RateLimiter rateLimiter = new RateLimiter();
        IntStream.range(0, 1000).forEach(i -> {
            try {
                if(rateLimiter.acquire(API_ID)) {
                    caller.call();
                } else {
                    System.out.println("failed to acquire..");
                }
            } catch (Exception e) {
                System.out.println("Exception occurred : " + e.getMessage());
            }

            long betweenCalls = (long) (random.nextDouble() * 300);
            try {
                Thread.sleep(150 + betweenCalls);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }
}