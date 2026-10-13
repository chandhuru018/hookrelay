package com.hookrelay.hookrelay.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = "hookrelay.worker.enabled=false")
class RetentionServiceTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private RetentionService retentionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Instant veryOld = Instant.now().minus(Duration.ofDays(400));
    private final Instant recent = Instant.now().minus(Duration.ofDays(1));
    private final Instant cutoff = Instant.now().minus(Duration.ofDays(399));

    private Long tenantId;
    private Long endpointId;

    private Long oldDeliveredEvent;
    private Long oldDeadEvent;
    private Long oldPendingEvent;
    private Long oldEventWithoutDeliveries;
    private Long recentEvent;

    private Long oldDelivered;
    private Long oldDead;
    private Long oldPending;
    private Long recentDelivered;

    @BeforeEach
    void seed() {
        tenantId = jdbcTemplate.queryForObject(
                "INSERT INTO tenants (name, api_key_hash) VALUES (?, ?) RETURNING id",
                Long.class, "Retention Test Tenant", "hash-" + UUID.randomUUID());
        endpointId = jdbcTemplate.queryForObject(
                "INSERT INTO endpoints (tenant_id, url, secret) VALUES (?, ?, ?) RETURNING id",
                Long.class, tenantId, "http://example.com", "secret");

        oldDeliveredEvent = insertEvent(veryOld);
        oldDeadEvent = insertEvent(veryOld);
        oldPendingEvent = insertEvent(veryOld);
        oldEventWithoutDeliveries = insertEvent(veryOld);
        recentEvent = insertEvent(recent);

        oldDelivered = insertDelivery(oldDeliveredEvent, "DELIVERED", veryOld);
        oldDead = insertDelivery(oldDeadEvent, "DEAD", veryOld);
        oldPending = insertDelivery(oldPendingEvent, "PENDING", veryOld);
        recentDelivered = insertDelivery(recentEvent, "DELIVERED", recent);

        jdbcTemplate.update(
                "INSERT INTO delivery_attempts (delivery_id, http_status, latency_ms) VALUES (?, 200, 12)",
                oldDelivered);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update(
                "DELETE FROM delivery_attempts WHERE delivery_id IN "
                        + "(SELECT d.id FROM deliveries d JOIN events e ON e.id = d.event_id WHERE e.tenant_id = ?)",
                tenantId);
        jdbcTemplate.update(
                "DELETE FROM deliveries WHERE event_id IN (SELECT id FROM events WHERE tenant_id = ?)",
                tenantId);
        jdbcTemplate.update("DELETE FROM events WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM endpoints WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM tenants WHERE id = ?", tenantId);
    }

    private Long insertEvent(Instant createdAt) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO events (tenant_id, type, payload, idempotency_key, created_at) "
                        + "VALUES (?, 'retention.test', '{}'::jsonb, ?, ?) RETURNING id",
                Long.class, tenantId, "retention-" + UUID.randomUUID(), Timestamp.from(createdAt));
    }

    private Long insertDelivery(Long eventId, String status, Instant createdAt) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO deliveries (event_id, endpoint_id, status, created_at) "
                        + "VALUES (?, ?, ?, ?) RETURNING id",
                Long.class, eventId, endpointId, status, Timestamp.from(createdAt));
    }

    private boolean exists(String table, Long id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    @Test
    void purgesOldFinishedDeliveriesAndOrphanedEventsOnly() {
        retentionService.purgeOlderThan(cutoff);

        assertFalse(exists("deliveries", oldDelivered));
        assertFalse(exists("deliveries", oldDead));
        assertFalse(exists("events", oldDeliveredEvent));
        assertFalse(exists("events", oldDeadEvent));
        assertFalse(exists("events", oldEventWithoutDeliveries));

        Integer attemptsLeft = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM delivery_attempts WHERE delivery_id = ?", Integer.class, oldDelivered);
        assertEquals(0, attemptsLeft);

        assertTrue(exists("deliveries", oldPending));
        assertTrue(exists("events", oldPendingEvent));
        assertTrue(exists("deliveries", recentDelivered));
        assertTrue(exists("events", recentEvent));
    }

    @Test
    void reportsHowManyRowsWerePurged() {
        RetentionService.PurgeResult result = retentionService.purgeOlderThan(cutoff);

        assertEquals(2, result.deliveries());
        assertEquals(3, result.events());
    }
}