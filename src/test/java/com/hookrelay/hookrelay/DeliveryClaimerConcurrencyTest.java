package com.hookrelay.hookrelay;

import com.hookrelay.hookrelay.worker.DeliveryClaimer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "hookrelay.worker.enabled=false")
class DeliveryClaimerConcurrencyTest {

    static {
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private DeliveryClaimer deliveryClaimer;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final int TOTAL_DELIVERIES = 100;
    private static final int WORKER_COUNT = 8;

    private Long testTenantId;
    private Long testEndpointId;
    private Long testEventId;

    @BeforeEach
    void setUp() {
        testTenantId = jdbcTemplate.queryForObject(
                "INSERT INTO tenants (name, api_key_hash) VALUES (?, ?) RETURNING id",
                Long.class, "Concurrency Test Tenant", "hash-" + java.util.UUID.randomUUID());

        testEndpointId = jdbcTemplate.queryForObject(
                "INSERT INTO endpoints (tenant_id, url, secret) VALUES (?, ?, ?) RETURNING id",
                Long.class, testTenantId, "http://example.com", "secret");

        testEventId = jdbcTemplate.queryForObject(
                "INSERT INTO events (tenant_id, type, payload, idempotency_key) VALUES (?, ?, ?::jsonb, ?) RETURNING id",
                Long.class, testTenantId, "test.event", "{}", "concurrency-key-" + java.util.UUID.randomUUID());

        for (int i = 0; i < TOTAL_DELIVERIES; i++) {
            jdbcTemplate.update(
                    "INSERT INTO deliveries (event_id, endpoint_id, status, next_attempt_at) VALUES (?, ?, 'PENDING', ?)",
                    testEventId, testEndpointId, java.sql.Timestamp.from(Instant.now()));
        }
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM delivery_attempts WHERE delivery_id IN (SELECT id FROM deliveries WHERE event_id = ?)",
                testEventId);
        jdbcTemplate.update("DELETE FROM deliveries WHERE event_id = ?", testEventId);
        jdbcTemplate.update("DELETE FROM events WHERE id = ?", testEventId);
        jdbcTemplate.update("DELETE FROM endpoints WHERE id = ?", testEndpointId);
        jdbcTemplate.update("DELETE FROM tenants WHERE id = ?", testTenantId);
    }

    @Test
    void multipleWorkersNeverClaimTheSameDelivery() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(WORKER_COUNT);
        List<Callable<List<Long>>> tasks = new java.util.ArrayList<>();

        for (int i = 0; i < WORKER_COUNT; i++) {
            tasks.add(() -> deliveryClaimer.claimBatch(20));
        }

        List<Future<List<Long>>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        List<Long> allClaimedIds = new CopyOnWriteArrayList<>();
        for (Future<List<Long>> future : futures) {
            allClaimedIds.addAll(future.get());
        }

        Set<Long> uniqueIds = Set.copyOf(allClaimedIds);

        assertEquals(allClaimedIds.size(), uniqueIds.size(),
                "Every claimed delivery id must be unique across all workers - no duplicates allowed");

        assertTrue(allClaimedIds.size() <= TOTAL_DELIVERIES,
                "Cannot claim more deliveries than exist");
    }
}