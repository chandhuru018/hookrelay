package com.hookrelay.hookrelay.worker;

import com.hookrelay.hookrelay.service.DeliveryExecutionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "hookrelay.worker.enabled", havingValue = "true", matchIfMissing = true)
public class DeliveryWorker {

    private static final int BATCH_SIZE = 20;

    private final DeliveryClaimer deliveryClaimer;
    private final DeliveryExecutionService deliveryExecutionService;

    public DeliveryWorker(DeliveryClaimer deliveryClaimer,
                          DeliveryExecutionService deliveryExecutionService) {
        this.deliveryClaimer = deliveryClaimer;
        this.deliveryExecutionService = deliveryExecutionService;
    }

    @Scheduled(fixedDelay = 5000)
    public void pollAndDeliver() {
        List<Long> claimedIds = deliveryClaimer.claimBatch(BATCH_SIZE);

        for (Long id : claimedIds) {
            deliveryExecutionService.executeDelivery(id);
        }
    }
}