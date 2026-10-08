package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Delivery;
import com.hookrelay.hookrelay.domain.DeliveryStatus;
import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.Event;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryReplayServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryReplayService deliveryReplayService;

    private Tenant caller;

    @BeforeEach
    void setUp() {
        caller = tenantWithId(1L);
    }

    private Tenant tenantWithId(Long id) {
        Tenant tenant = new Tenant("Tenant " + id, "hash-" + id);
        ReflectionTestUtils.setField(tenant, "id", id);
        return tenant;
    }

    private Delivery deliveryOwnedBy(Tenant owner, DeliveryStatus status) {
        Event event = new Event(owner, "order.paid", "{}", "key-" + owner.getId());
        Endpoint endpoint = new Endpoint(owner, "http://example.com", "secret");
        Delivery delivery = new Delivery(event, endpoint);
        delivery.setStatus(status);
        delivery.setAttemptCount(6);
        delivery.setLastError("connection refused");
        return delivery;
    }

    @Test
    void replay_resetsDeadDeliveryToPending() {
        Delivery dead = deliveryOwnedBy(caller, DeliveryStatus.DEAD);
        when(deliveryRepository.findById(10L)).thenReturn(Optional.of(dead));
        when(deliveryRepository.save(any(Delivery.class))).thenAnswer(inv -> inv.getArgument(0));

        Delivery result = deliveryReplayService.replay(caller, 10L);

        assertEquals(DeliveryStatus.PENDING, result.getStatus());
        assertEquals(0, result.getAttemptCount());
        assertNull(result.getLastError());
    }

    @Test
    void replay_throwsConflict_whenDeliveryIsNotDead() {
        Delivery retrying = deliveryOwnedBy(caller, DeliveryStatus.RETRYING);
        when(deliveryRepository.findById(10L)).thenReturn(Optional.of(retrying));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> deliveryReplayService.replay(caller, 10L));

        assertEquals(409, ex.getStatusCode().value());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void replay_throwsNotFound_whenDeliveryBelongsToAnotherTenant() {
        Tenant otherTenant = tenantWithId(2L);
        Delivery othersDelivery = deliveryOwnedBy(otherTenant, DeliveryStatus.DEAD);
        when(deliveryRepository.findById(10L)).thenReturn(Optional.of(othersDelivery));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> deliveryReplayService.replay(caller, 10L));

        assertEquals(404, ex.getStatusCode().value());
        verify(deliveryRepository, never()).save(any());
    }

    @Test
    void replay_throwsNotFound_whenDeliveryDoesNotExist() {
        when(deliveryRepository.findById(99999L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> deliveryReplayService.replay(caller, 99999L));

        assertEquals(404, ex.getStatusCode().value());
    }
}