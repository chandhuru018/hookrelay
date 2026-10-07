package com.hookrelay.hookrelay.worker;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class QueueDepthMetrics {

    public QueueDepthMetrics(MeterRegistry registry, JdbcTemplate jdbcTemplate) {
        Gauge.builder("hookrelay.queue.depth", () -> jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM deliveries WHERE status IN ('PENDING', 'RETRYING')",
                        Long.class))
                .description("Deliveries waiting to be sent")
                .register(registry);
    }
}