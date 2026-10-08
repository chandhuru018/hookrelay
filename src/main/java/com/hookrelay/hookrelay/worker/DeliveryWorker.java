package com.hookrelay.hookrelay.worker;

import com.hookrelay.hookrelay.service.DeliveryExecutionService;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@ConditionalOnProperty(name = "hookrelay.worker.enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryWorker {

    private static final Logger log = LoggerFactory.getLogger(DeliveryWorker.class);

    private static final int BATCH_SIZE = 50;
    private static final int PARALLELISM = 8;

    private final DeliveryClaimer deliveryClaimer;
    private final DeliveryExecutionService deliveryExecutionService;
    private final ExecutorService executor = Executors.newFixedThreadPool(PARALLELISM);

    public DeliveryWorker(DeliveryClaimer deliveryClaimer,
                          DeliveryExecutionService deliveryExecutionService) {
        this.deliveryClaimer = deliveryClaimer;
        this.deliveryExecutionService = deliveryExecutionService;
    }

    @Scheduled(fixedDelay = 1000)
    public void pollAndDeliver() {
        List<Long> claimedIds;
        do {
            claimedIds = deliveryClaimer.claimBatch(BATCH_SIZE);

            List<CompletableFuture<Void>> futures = claimedIds.stream()
                    .map(id -> CompletableFuture.runAsync(() -> deliver(id), executor))
                    .toList();

            futures.forEach(CompletableFuture::join);
        } while (!claimedIds.isEmpty());
    }

    private void deliver(Long id) {
        try {
            deliveryExecutionService.executeDelivery(id);
        } catch (Exception e) {
            log.error("Unexpected failure delivering {}", id, e);
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}