package com.hookrelay.hookrelay.worker;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "hookrelay.worker.enabled", havingValue = "true", matchIfMissing = true)
public class StuckDeliveryRecovery {

    private static final Logger log = LoggerFactory.getLogger(StuckDeliveryRecovery.class);

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;

    public StuckDeliveryRecovery(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Scheduled(fixedDelay = 60000)
    public void recoverStuckDeliveries() {
        int recovered = jdbcTemplate.update("""
                UPDATE deliveries
                SET status = 'PENDING', next_attempt_at = now(), claimed_at = NULL
                WHERE status = 'IN_FLIGHT'
                  AND (claimed_at IS NULL OR claimed_at < now() - interval '5 minutes')
                """);

        if (recovered > 0) {
            log.warn("Recovered {} deliveries stuck IN_FLIGHT", recovered);
            meterRegistry.counter("hookrelay.deliveries.recovered").increment(recovered);
        }
    }
}