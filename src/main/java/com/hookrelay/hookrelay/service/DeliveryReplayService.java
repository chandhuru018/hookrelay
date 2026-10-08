package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Delivery;
import com.hookrelay.hookrelay.domain.DeliveryStatus;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class DeliveryReplayService {

    private final DeliveryRepository deliveryRepository;

    public DeliveryReplayService(DeliveryRepository deliveryRepository) {
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public Delivery replay(Tenant tenant, Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .filter(d -> d.getEvent().getTenant().getId().equals(tenant.getId()))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Delivery not found"));

        if (delivery.getStatus() != DeliveryStatus.DEAD) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Only DEAD deliveries can be replayed (current status: "
                            + delivery.getStatus() + ")");
        }

        delivery.setStatus(DeliveryStatus.PENDING);
        delivery.setAttemptCount(0);
        delivery.setNextAttemptAt(Instant.now());
        delivery.setLastError(null);

        return deliveryRepository.save(delivery);
    }
}