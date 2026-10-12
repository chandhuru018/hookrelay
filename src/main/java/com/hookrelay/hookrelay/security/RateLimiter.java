package com.hookrelay.hookrelay.security;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

public class RateLimiter {

    public record Decision(boolean allowed, long retryAfterSeconds) {
    }

    private static final int MAX_TRACKED_KEYS = 100_000;

    private static final class Bucket {
        double tokens;
        long lastRefillNanos;

        Bucket(double tokens, long nowNanos) {
            this.tokens = tokens;
            this.lastRefillNanos = nowNanos;
        }
    }

    private final int capacity;
    private final double nanosPerToken;
    private final LongSupplier nanoClock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter(int capacity, Duration window) {
        this(capacity, window, System::nanoTime);
    }

    public RateLimiter(int capacity, Duration window, LongSupplier nanoClock) {
        if (capacity < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("capacity must be >= 1 and window must be positive");
        }
        this.capacity = capacity;
        this.nanosPerToken = (double) window.toNanos() / capacity;
        this.nanoClock = nanoClock;
    }

    public Decision tryAcquire(String key) {
        if (!buckets.containsKey(key) && buckets.size() >= MAX_TRACKED_KEYS) {
            evictIdle();
            if (buckets.size() >= MAX_TRACKED_KEYS) {
                return new Decision(false, 60);
            }
        }

        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity, nanoClock.getAsLong()));

        synchronized (bucket) {
            refill(bucket);
            if (bucket.tokens >= 1.0) {
                bucket.tokens -= 1.0;
                return new Decision(true, 0);
            }
            long seconds = (long) Math.ceil((1.0 - bucket.tokens) * nanosPerToken / 1_000_000_000.0);
            return new Decision(false, Math.max(1, seconds));
        }
    }

    public void evictIdle() {
        buckets.entrySet().removeIf(entry -> {
            Bucket bucket = entry.getValue();
            synchronized (bucket) {
                refill(bucket);
                return bucket.tokens >= capacity;
            }
        });
    }

    public int trackedKeys() {
        return buckets.size();
    }

    private void refill(Bucket bucket) {
        long now = nanoClock.getAsLong();
        long elapsed = now - bucket.lastRefillNanos;
        if (elapsed > 0) {
            bucket.tokens = Math.min(capacity, bucket.tokens + elapsed / nanosPerToken);
            bucket.lastRefillNanos = now;
        }
    }
}