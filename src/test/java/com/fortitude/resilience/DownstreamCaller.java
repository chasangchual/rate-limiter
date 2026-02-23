package com.fortitude.resilience;

import java.util.Random;
import java.util.concurrent.Callable;

public class DownstreamCaller implements Callable<Boolean> {
    Random random = new Random();

    @Override
    public Boolean call() throws Exception {
        long now = System.nanoTime();
        System.out.println("call the downstream at " + now);
        long executionTime = (long) (random.nextDouble() * 100);
        Thread.sleep(executionTime);
        return Boolean.TRUE;
    }
}
