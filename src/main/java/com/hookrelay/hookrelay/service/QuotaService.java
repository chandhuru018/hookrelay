package com.hookrelay.hookrelay.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class QuotaService {

    private final JdbcTemplate jdbcTemplate;
    private final int maxEndpointsPerTenant;
    private final int maxStoredEventsPerTenant;
    private final long maxTotalEvents;
    private final int maxPayloadBytes;

    public QuotaService(
            JdbcTemplate jdbcTemplate,
            @Value("${hookrelay.limits.max-endpoints-per-tenant:0}") int maxEndpointsPerTenant,
            @Value("${hookrelay.limits.max-stored-events-per-tenant:0}") int maxStoredEventsPerTenant,
            @Value("${hookrelay.limits.max-total-events:0}") long maxTotalEvents,
            @Value("${hookrelay.limits.max-payload-bytes:0}") int maxPayloadBytes) {
        this.jdbcTemplate = jdbcTemplate;
        this.maxEndpointsPerTenant = maxEndpointsPerTenant;
        this.maxStoredEventsPerTenant = maxStoredEventsPerTenant;
        this.maxTotalEvents = maxTotalEvents;
        this.maxPayloadBytes = maxPayloadBytes;
    }

    public void checkCanRegisterEndpoint(Long tenantId) {
        if (maxEndpointsPerTenant > 0
                && boundedCount("SELECT 1 FROM endpoints WHERE tenant_id = ?", maxEndpointsPerTenant, tenantId)
                >= maxEndpointsPerTenant) {
            throw new QuotaExceededException(
                    "Endpoint limit reached (" + maxEndpointsPerTenant + " per tenant)", 429);
        }
    }

    public void checkCanPublish(Long tenantId, String payload) {
        if (maxPayloadBytes > 0 && payload != null
                && payload.getBytes(StandardCharsets.UTF_8).length > maxPayloadBytes) {
            throw new InvalidRequestException("payload must be at most " + maxPayloadBytes + " bytes");
        }

        if (maxStoredEventsPerTenant > 0
                && boundedCount("SELECT 1 FROM events WHERE tenant_id = ?", maxStoredEventsPerTenant, tenantId)
                >= maxStoredEventsPerTenant) {
            throw new QuotaExceededException(
                    "Stored event limit reached for this tenant (" + maxStoredEventsPerTenant
                            + "); older events are removed automatically", 429);
        }

        if (maxTotalEvents > 0
                && boundedCount("SELECT 1 FROM events", maxTotalEvents) >= maxTotalEvents) {
            throw new QuotaExceededException(
                    "The demo storage is full right now. Please try again later", 503);
        }
    }

    private long boundedCount(String selectOne, long cap, Object... args) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM (" + selectOne + " LIMIT " + cap + ") bounded", Long.class, args);
        return count == null ? 0 : count;
    }
}