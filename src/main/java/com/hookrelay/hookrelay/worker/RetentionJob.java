package com.hookrelay.hookrelay.worker;

import com.hookrelay.hookrelay.service.RetentionService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Component
@ConditionalOnProperty(name = "hookrelay.worker.enabled", havingValue = "true", matchIfMissing = true)
public class RetentionJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionJob.class);

    private final RetentionService retentionService;
    private final MeterRegistry meterRegistry;
    private final long retentionDays;

    public RetentionJob(RetentionService retentionService,
                        MeterRegistry meterRegistry,
                        @Value("${hookrelay.retention.days:7}") long retentionDays) {
        this.retentionService = retentionService;
        this.meterRegistry = meterRegistry;
        this.retentionDays = retentionDays;
    }

    @Scheduled(
            fixedDelayString = "${hookrelay.retention.interval-ms:3600000}",
            initialDelayString = "${hookrelay.retention.initial-delay-ms:60000}")
    public void purgeOldData() {
        if (retentionDays <= 0) {
            return;
        }

        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        RetentionService.PurgeResult result = retentionService.purgeOlderThan(cutoff);

        if (result.deliveries() > 0 || result.events() > 0) {
            log.info("Retention purge removed {} deliveries and {} events older than {} days",
                    result.deliveries(), result.events(), retentionDays);
            meterRegistry.counter("hookrelay.retention.purged", "type", "deliveries")
                    .increment(result.deliveries());
            meterRegistry.counter("hookrelay.retention.purged", "type", "events")
                    .increment(result.events());
        }
    }
}