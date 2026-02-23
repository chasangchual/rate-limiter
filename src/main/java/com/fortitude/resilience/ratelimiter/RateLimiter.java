package com.fortitude.resilience.ratelimiter;

import java.util.HashMap;
import java.util.Map;

/**
 * limit # of calls
 */
public class RateLimiter {
    public static final String API_1 = "API_0000001";
    public static final String API_2 = "API_0000002";
    public static final long ONE_SEC =  1L * 1000 * 1000 * 1000;
    public RateLimiterConfig defaultConfig;

    Map<String, RateLimiterConfig> config = new HashMap<>();

    Map<String, Integer> tokenBucket = new HashMap<>();
    Map<String, Long> tokenRefilledAt = new HashMap<>();

    public RateLimiter() {
        defaultConfig = new RateLimiterConfig(1, 2, ONE_SEC, 2);

        config.put(API_1, new RateLimiterConfig(1, 3, ONE_SEC, 2));
        config.put(API_2, new RateLimiterConfig(2, 4, ONE_SEC, 2));

        long now = System.nanoTime();

        tokenBucket.put(API_1, config.get(API_1).initialToken());
        tokenBucket.put(API_2, config.get(API_2).initialToken());

        tokenRefilledAt.put(API_1, now);
        tokenRefilledAt.put(API_2, now);
    }

    public synchronized boolean acquire(String apiId) {
        int tokensPerCall = config.getOrDefault(apiId, defaultConfig).tokesPerCall();
        int currentTokens = tokenBucket.getOrDefault(apiId, 1);

        if(currentTokens >= tokensPerCall) {
            if(tokenBucket.containsKey(apiId)) {
                tokenBucket.put(apiId, currentTokens - tokensPerCall);
            }
            return true;
        } else {
            if(tokenRefilledAt.containsKey(apiId)) {
                long lastRefilledAt = tokenRefilledAt.get(apiId);
                long refillPeriod = config.get(apiId).refillPeriod();
                long now = System.nanoTime();

                if((now - lastRefilledAt) > refillPeriod) {
                    int tokensRefill = config.get(apiId).tokensToRefill();

                    tokenBucket.put(apiId, tokensRefill);
                    tokenRefilledAt.put(apiId, now);
                }
            }
        }
        return false;
    }
}
