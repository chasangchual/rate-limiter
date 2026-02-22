package com.fortitude.circuitbreaker;

import java.util.Random;
import java.util.concurrent.Callable;

public class DownstreamCaller implements Callable<Boolean> {
    Random random = new Random();

    @Override
    public Boolean call() throws Exception {
        if(random.nextDouble() > 0.8d) {
            throw new RuntimeException("failed to call the downstream");
        }

        if(random.nextDouble() > 0.8d) {
            System.out.println("Downstream client is getting slow");
            Thread.sleep(800);
        }

        long now = System.nanoTime();
        System.out.println("call the downstream at " + now);
        long executionTime = (long) (random.nextDouble() * 100);
        Thread.sleep(executionTime);
        return Boolean.TRUE;
    }
}
