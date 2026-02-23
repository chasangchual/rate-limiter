package com.fortitude.resilience.ratelimiter;

public record RateLimiterConfig(int tokesPerCall, int tokensToRefill, long refillPeriod, int initialToken) {
}
