package com.hookrelay.hookrelay.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.TimeZone;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "hookrelay.worker.enabled=false")
class QuotaServiceTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long tenantId;

    @BeforeEach
    void seed() {
        tenantId = jdbcTemplate.queryForObject(
                "INSERT INTO tenants (name, api_key_hash) VALUES (?, ?) RETURNING id",
                Long.class, "Quota Test Tenant", "hash-" + UUID.randomUUID());

        for (int i = 0; i < 2; i++) {
            jdbcTemplate.update(
                    "INSERT INTO endpoints (tenant_id, url, secret) VALUES (?, ?, ?)",
                    tenantId, "http://example.com/" + i, "secret");
        }
        for (int i = 0; i < 3; i++) {
            jdbcTemplate.update(
                    "INSERT INTO events (tenant_id, type, payload, idempotency_key) "
                            + "VALUES (?, 'quota.test', '{}'::jsonb, ?)",
                    tenantId, "quota-" + UUID.randomUUID());
        }
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM events WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM endpoints WHERE tenant_id = ?", tenantId);
        jdbcTemplate.update("DELETE FROM tenants WHERE id = ?", tenantId);
    }

    private QuotaService quota(int endpoints, int eventsPerTenant, long totalEvents, int payloadBytes) {
        return new QuotaService(jdbcTemplate, endpoints, eventsPerTenant, totalEvents, payloadBytes);
    }

    @Test
    void rejectsNewEndpointAtTheLimit() {
        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
                () -> quota(2, 0, 0, 0).checkCanRegisterEndpoint(tenantId));

        assertEquals(429, ex.getStatus());
        assertDoesNotThrow(() -> quota(3, 0, 0, 0).checkCanRegisterEndpoint(tenantId));
    }

    @Test
    void rejectsPublishWhenTenantHasTooManyStoredEvents() {
        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
                () -> quota(0, 3, 0, 0).checkCanPublish(tenantId, "{}"));

        assertEquals(429, ex.getStatus());
        assertDoesNotThrow(() -> quota(0, 4, 0, 0).checkCanPublish(tenantId, "{}"));
    }

    @Test
    void rejectsPublishWhenWholeDatabaseIsAtCapacity() {
        QuotaExceededException ex = assertThrows(QuotaExceededException.class,
                () -> quota(0, 0, 1, 0).checkCanPublish(tenantId, "{}"));

        assertEquals(503, ex.getStatus());
    }

    @Test
    void measuresPayloadSizeInBytesNotCharacters() {
        QuotaService limited = quota(0, 0, 0, 10);

        assertDoesNotThrow(() -> limited.checkCanPublish(tenantId, "{}"));
        assertThrows(InvalidRequestException.class,
                () -> limited.checkCanPublish(tenantId, "\u00e9".repeat(6)));
    }

    @Test
    void everyLimitIsOffWhenSetToZero() {
        QuotaService unlimited = quota(0, 0, 0, 0);

        assertDoesNotThrow(() -> unlimited.checkCanRegisterEndpoint(tenantId));
        assertDoesNotThrow(() -> unlimited.checkCanPublish(tenantId, "x".repeat(100_000)));
    }
}