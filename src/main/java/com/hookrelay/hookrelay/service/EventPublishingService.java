package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Delivery;
import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.EndpointStatus;
import com.hookrelay.hookrelay.domain.Event;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import com.hookrelay.hookrelay.repository.EndpointRepository;
import com.hookrelay.hookrelay.repository.EventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EventPublishingService {

    private final EventRepository eventRepository;
    private final EndpointRepository endpointRepository;
    private final DeliveryRepository deliveryRepository;

    public EventPublishingService(EventRepository eventRepository,
                                  EndpointRepository endpointRepository,
                                  DeliveryRepository deliveryRepository) {
        this.eventRepository = eventRepository;
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
    }

    @Transactional
    public Event publishEvent(Tenant tenant, String type, String payload, String idempotencyKey) {
        Event event = new Event(tenant, type, payload, idempotencyKey);

        try {
            event = eventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateEventException(
                    "Event with idempotencyKey '" + idempotencyKey + "' already exists for this tenant");
        }

        List<Endpoint> endpoints = endpointRepository.findByTenantId(tenant.getId())
                .stream()
                .filter(endpoint -> endpoint.getStatus() == EndpointStatus.ACTIVE)
                .toList();

        for (Endpoint endpoint : endpoints) {
            Delivery delivery = new Delivery(event, endpoint);
            deliveryRepository.save(delivery);
        }

        return event;
    }
}