package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.api.DeliveryStatsResponse;
import com.hookrelay.hookrelay.domain.Delivery;
import com.hookrelay.hookrelay.domain.DeliveryStatus;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeliveryStatsService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryStatsService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    public DeliveryStatsResponse getStats(Long tenantId) {
        List<Delivery> deliveries = deliveryRepository.findAllForTenant(tenantId);

        long total = deliveries.size();

        Map<String, Long> countByStatus = deliveries.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getStatus().name(),
                        Collectors.counting()));

        long delivered = countByStatus.getOrDefault(DeliveryStatus.DELIVERED.name(), 0L);

        double successRate = total == 0 ? 0.0 : (delivered * 100.0) / total;

        return new DeliveryStatsResponse(total, countByStatus, successRate);
    }
}