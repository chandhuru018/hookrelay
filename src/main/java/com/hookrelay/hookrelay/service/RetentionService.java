package com.hookrelay.hookrelay.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Service
public class RetentionService {

    public record PurgeResult(int deliveries, int events) {
    }

    private static final int BATCH_SIZE = 1000;

    private static final String FINISHED_DELIVERIES_SQL = """
            SELECT id FROM deliveries
            WHERE status IN ('DELIVERED', 'DEAD')
              AND created_at < ?
            ORDER BY id
            LIMIT ?
            FOR UPDATE SKIP LOCKED
            """;

    private static final String ORPHANED_EVENTS_SQL = """
            SELECT e.id FROM events e
            WHERE e.created_at < ?
              AND NOT EXISTS (SELECT 1 FROM deliveries d WHERE d.event_id = e.id)
            ORDER BY e.id
            LIMIT ?
            FOR UPDATE OF e SKIP LOCKED
            """;

    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    public RetentionService(JdbcTemplate jdbcTemplate, TransactionTemplate transactionTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    public PurgeResult purgeOlderThan(Instant cutoff) {
        int deliveries = 0;
        int batch;
        do {
            batch = purgeFinishedDeliveries(cutoff);
            deliveries += batch;
        } while (batch > 0);

        int events = 0;
        do {
            batch = purgeOrphanedEvents(cutoff);
            events += batch;
        } while (batch > 0);

        return new PurgeResult(deliveries, events);
    }

    private int purgeFinishedDeliveries(Instant cutoff) {
        Integer purged = transactionTemplate.execute(status -> {
            List<Long> ids = jdbcTemplate.queryForList(
                    FINISHED_DELIVERIES_SQL, Long.class, Timestamp.from(cutoff), BATCH_SIZE);
            if (ids.isEmpty()) {
                return 0;
            }
            Long[] idArray = ids.toArray(new Long[0]);
            jdbcTemplate.update("DELETE FROM delivery_attempts WHERE delivery_id = ANY(?)", (Object) idArray);
            jdbcTemplate.update("DELETE FROM deliveries WHERE id = ANY(?)", (Object) idArray);
            return ids.size();
        });
        return purged == null ? 0 : purged;
    }

    private int purgeOrphanedEvents(Instant cutoff) {
        Integer purged = transactionTemplate.execute(status -> {
            List<Long> ids = jdbcTemplate.queryForList(
                    ORPHANED_EVENTS_SQL, Long.class, Timestamp.from(cutoff), BATCH_SIZE);
            if (ids.isEmpty()) {
                return 0;
            }
            jdbcTemplate.update("DELETE FROM events WHERE id = ANY(?)", (Object) ids.toArray(new Long[0]));
            return ids.size();
        });
        return purged == null ? 0 : purged;
    }
}