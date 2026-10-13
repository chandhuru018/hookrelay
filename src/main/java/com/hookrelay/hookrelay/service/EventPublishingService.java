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

import java.sql.SQLException;
import java.util.List;

@Service
public class EventPublishingService {

    private static final String UNIQUE_VIOLATION = "23505";
    private static final String INVALID_TEXT_REPRESENTATION = "22P02";
    private static final String UNTRANSLATABLE_CHARACTER = "22P05";

    private final EventRepository eventRepository;
    private final EndpointRepository endpointRepository;
    private final DeliveryRepository deliveryRepository;
    private final QuotaService quotaService;

    public EventPublishingService(EventRepository eventRepository,
                                  EndpointRepository endpointRepository,
                                  DeliveryRepository deliveryRepository,
                                  QuotaService quotaService) {
        this.eventRepository = eventRepository;
        this.endpointRepository = endpointRepository;
        this.deliveryRepository = deliveryRepository;
        this.quotaService = quotaService;
    }

    @Transactional
    public Event publishEvent(Tenant tenant, String type, String payload, String idempotencyKey) {
        quotaService.checkCanPublish(tenant.getId(), payload);

        Event event = new Event(tenant, type, payload, idempotencyKey);

        try {
            event = eventRepository.save(event);
        } catch (DataIntegrityViolationException e) {
            String sqlState = findSqlState(e);

            if (UNIQUE_VIOLATION.equals(sqlState)) {
                throw new DuplicateEventException(
                        "Event with idempotencyKey '" + idempotencyKey + "' already exists for this tenant");
            }
            if (INVALID_TEXT_REPRESENTATION.equals(sqlState) || UNTRANSLATABLE_CHARACTER.equals(sqlState)) {
                throw new InvalidRequestException("payload must be valid JSON");
            }
            throw e;
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

    private static String findSqlState(Throwable error) {
        Throwable current = error;
        for (int depth = 0; current != null && depth < 10; depth++) {
            if (current instanceof SQLException sqlException && sqlException.getSQLState() != null) {
                return sqlException.getSQLState();
            }
            current = current.getCause();
        }
        return null;
    }
}