package com.hookrelay.hookrelay.security;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    private static final long SECOND = 1_000_000_000L;

    private final AtomicLong clock = new AtomicLong(0);

    private RateLimiter limiter() {
        // 3 tokens per 30 seconds: one new token every 10 seconds
        return new RateLimiter(3, Duration.ofSeconds(30), clock::get);
    }

    @Test
    void allowsUpToLimitThenRejectsWithRetryAfter() {
        RateLimiter limiter = limiter();

        assertTrue(limiter.tryAcquire("a").allowed());
        assertTrue(limiter.tryAcquire("a").allowed());
        assertTrue(limiter.tryAcquire("a").allowed());

        RateLimiter.Decision rejected = limiter.tryAcquire("a");
        assertFalse(rejected.allowed());
        assertEquals(10, rejected.retryAfterSeconds());
    }

    @Test
    void refillsOverTime() {
        RateLimiter limiter = limiter();
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }
        assertFalse(limiter.tryAcquire("a").allowed());

        clock.addAndGet(10 * SECOND);

        assertTrue(limiter.tryAcquire("a").allowed());
        assertFalse(limiter.tryAcquire("a").allowed());
    }

    @Test
    void keysAreIndependent() {
        RateLimiter limiter = limiter();
        for (int i = 0; i < 3; i++) {
            limiter.tryAcquire("a");
        }

        assertFalse(limiter.tryAcquire("a").allowed());
        assertTrue(limiter.tryAcquire("b").allowed());
    }

    @Test
    void neverExceedsCapacityAfterLongIdle() {
        RateLimiter limiter = limiter();
        limiter.tryAcquire("a");

        clock.addAndGet(1000 * SECOND);

        assertTrue(limiter.tryAcquire("a").allowed());
        assertTrue(limiter.tryAcquire("a").allowed());
        assertTrue(limiter.tryAcquire("a").allowed());
        assertFalse(limiter.tryAcquire("a").allowed());
    }

    @Test
    void evictsFullyRefilledBuckets() {
        RateLimiter limiter = limiter();
        limiter.tryAcquire("a");
        assertEquals(1, limiter.trackedKeys());

        limiter.evictIdle();
        assertEquals(1, limiter.trackedKeys());

        clock.addAndGet(30 * SECOND);
        limiter.evictIdle();
        assertEquals(0, limiter.trackedKeys());
    }
}