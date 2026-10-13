package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.EndpointStatus;
import com.hookrelay.hookrelay.domain.Event;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import com.hookrelay.hookrelay.repository.EndpointRepository;
import com.hookrelay.hookrelay.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventPublishingServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private QuotaService quotaService;

    @InjectMocks
    private EventPublishingService eventPublishingService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = new Tenant("Test Tenant", "hash123");
    }

    private DataIntegrityViolationException violationWithSqlState(String message, String sqlState) {
        return new DataIntegrityViolationException(message, new SQLException(message, sqlState));
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

        verify(deliveryRepository, times(1)).save(any());
    }

    @Test
    void publishEvent_throwsDuplicateEventException_whenIdempotencyKeyAlreadyUsed() {
        when(eventRepository.save(any(Event.class)))
                .thenThrow(violationWithSqlState("duplicate key value violates unique constraint", "23505"));

        assertThrows(DuplicateEventException.class, () ->
                eventPublishingService.publishEvent(tenant, "order.paid", "{}", "duplicate-key"));

        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void publishEvent_throwsInvalidRequest_whenPayloadIsNotValidJson() {
        when(eventRepository.save(any(Event.class)))
                .thenThrow(violationWithSqlState("invalid input syntax for type json", "22P02"));

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                eventPublishingService.publishEvent(tenant, "order.paid", "not json", "key-3"));

        assertEquals("payload must be valid JSON", ex.getMessage());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void publishEvent_rethrowsOtherIntegrityViolations_insteadOfCallingThemDuplicates() {
        when(eventRepository.save(any(Event.class)))
                .thenThrow(violationWithSqlState("null value in column", "23502"));

        assertThrows(DataIntegrityViolationException.class, () ->
                eventPublishingService.publishEvent(tenant, "order.paid", "{}", "key-4"));

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

    @Test
    void publishEvent_rejectsBeforeSavingAnythingWhenQuotaIsExceeded() {
        doThrow(new QuotaExceededException("limit reached", 429))
                .when(quotaService).checkCanPublish(any(), any());

        assertThrows(QuotaExceededException.class, () ->
                eventPublishingService.publishEvent(tenant, "order.paid", "{}", "key-5"));

        verify(eventRepository, never()).save(any());
        verify(deliveryRepository, never()).save(any());
    }
}