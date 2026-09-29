package com.hookrelay.hookrelay.worker;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Component
public class DeliveryClaimer {

    private final JdbcTemplate jdbcTemplate;

    public DeliveryClaimer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Long> claimBatch(int batchSize) {
        String sql = """
                SELECT id FROM deliveries
                WHERE status IN ('PENDING', 'RETRYING')
                  AND next_attempt_at <= ?
                ORDER BY next_attempt_at
                FOR UPDATE SKIP LOCKED
                LIMIT ?
                """;

        List<Long> ids = jdbcTemplate.queryForList(
                sql, Long.class, Timestamp.from(Instant.now()), batchSize);

        if (ids.isEmpty()) {
            return ids;
        }

        jdbcTemplate.update(
                "UPDATE deliveries SET status = 'IN_FLIGHT' WHERE id = ANY(?)",
                (Object) ids.toArray(new Long[0]));

        return ids;
    }
}