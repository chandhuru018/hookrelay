package com.hookrelay.hookrelay;

import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.EndpointStatus;
import com.hookrelay.hookrelay.domain.Event;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import com.hookrelay.hookrelay.repository.EndpointRepository;
import com.hookrelay.hookrelay.repository.EventRepository;
import com.hookrelay.hookrelay.service.DuplicateEventException;
import com.hookrelay.hookrelay.service.EventPublishingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventPublishingServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private EventPublishingService eventPublishingService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant("Test Tenant", "hash123");
    }

    @Test
    void publishEvent_savesEventAndCreatesDeliveriesForActiveEndpointsOnly() {
        Endpoint activeEndpoint = new Endpoint(tenant, "http://active.com", "secret1");
        Endpoint disabledEndpoint = new Endpoint(tenant, "http://disabled.com", "secret2");
        disabledEndpoint.setStatus(EndpointStatus.DISABLED);

        Event savedEvent = new Event(tenant, "order.paid", "{}", "key-1");

        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(endpointRepository.findByTenantId(any())).thenReturn(
                List.of(activeEndpoint, disabledEndpoint));

        eventPublishingService.publishEvent(tenant, "order.paid", "{}", "key-1");

        // Only ONE delivery should be created: for the active endpoint only
        verify(deliveryRepository, times(1)).save(any());
    }

    @Test
    void publishEvent_throwsDuplicateEventException_whenIdempotencyKeyAlreadyUsed() {
        when(eventRepository.save(any(Event.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(DuplicateEventException.class, () ->
                eventPublishingService.publishEvent(tenant, "order.paid", "{}", "duplicate-key"));

        // No deliveries should ever be created for a rejected event
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void publishEvent_createsNoDeliveries_whenTenantHasNoEndpoints() {
        Event savedEvent = new Event(tenant, "order.paid", "{}", "key-2");

        when(eventRepository.save(any(Event.class))).thenReturn(savedEvent);
        when(endpointRepository.findByTenantId(any())).thenReturn(List.of());

        eventPublishingService.publishEvent(tenant, "order.paid", "{}", "key-2");

        verify(deliveryRepository, never()).save(any());
    }
}