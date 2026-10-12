package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Delivery;
import com.hookrelay.hookrelay.domain.DeliveryStatus;
import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.EndpointStatus;
import com.hookrelay.hookrelay.domain.Event;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import com.hookrelay.hookrelay.repository.EndpointRepository;
import com.hookrelay.hookrelay.security.UrlSafetyValidator;
import com.hookrelay.hookrelay.security.WebhookSigner;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryExecutionSsrfTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @Mock
    private EndpointRepository endpointRepository;

    @Test
    void blocksDeliveryToInternalAddress_marksDeadAndDisablesEndpoint() {
        Tenant tenant = new Tenant("Tenant", "hash");
        Endpoint endpoint = new Endpoint(tenant, "http://169.254.169.254/latest/meta-data", "secret");
        Event event = new Event(tenant, "order.paid", "{}", "key-1");
        Delivery delivery = new Delivery(event, endpoint);

        when(deliveryRepository.findById(1L)).thenReturn(Optional.of(delivery));

        SimpleMeterRegistry meters = new SimpleMeterRegistry();
        DeliveryExecutionService service = new DeliveryExecutionService(
                deliveryRepository, endpointRepository, new WebhookSigner(),
                meters, new UrlSafetyValidator(false));

        service.executeDelivery(1L);

        assertEquals(DeliveryStatus.DEAD, delivery.getStatus());
        assertEquals(EndpointStatus.DISABLED, endpoint.getStatus());
        assertTrue(delivery.getLastError().startsWith("Blocked:"));
        assertEquals(1.0, meters.counter("hookrelay.deliveries", "outcome", "blocked").count());
        verify(endpointRepository).save(endpoint);
    }
}