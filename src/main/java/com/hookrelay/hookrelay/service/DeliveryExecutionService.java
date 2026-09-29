package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Delivery;
import com.hookrelay.hookrelay.domain.DeliveryStatus;
import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.EndpointStatus;
import com.hookrelay.hookrelay.domain.Event;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import com.hookrelay.hookrelay.repository.EndpointRepository;
import com.hookrelay.hookrelay.security.WebhookSigner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class DeliveryExecutionService {

    private static final int MAX_ATTEMPTS = 6;

    private final DeliveryRepository deliveryRepository;
    private final EndpointRepository endpointRepository;
    private final WebhookSigner webhookSigner;
    private final RestClient restClient;

    public DeliveryExecutionService(DeliveryRepository deliveryRepository,
                                    EndpointRepository endpointRepository,
                                    WebhookSigner webhookSigner) {
        this.deliveryRepository = deliveryRepository;
        this.endpointRepository = endpointRepository;
        this.webhookSigner = webhookSigner;
        this.restClient = RestClient.builder().build();
    }

    @Transactional
    public void executeDelivery(Long deliveryId) {
        Optional<Delivery> maybeDelivery = deliveryRepository.findById(deliveryId);
        if (maybeDelivery.isEmpty()) {
            return;
        }

        Delivery delivery = maybeDelivery.get();
        Endpoint endpoint = delivery.getEndpoint();
        Event event = delivery.getEvent();

        String body = event.getPayload();
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String signature = webhookSigner.sign(endpoint.getSecret(), timestamp, body);

        try {
            restClient.post()
                    .uri(endpoint.getUrl())
                    .header("Content-Type", "application/json")
                    .header("X-HookRelay-Timestamp", timestamp)
                    .header("X-HookRelay-Signature", signature)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();

            delivery.setStatus(DeliveryStatus.DELIVERED);
            resetEndpointFailures(endpoint);

        } catch (Exception e) {
            handleFailure(delivery, endpoint, e.getMessage());
        }

        deliveryRepository.save(delivery);
    }

    private void handleFailure(Delivery delivery, Endpoint endpoint, String errorMessage) {
        int attempts = delivery.getAttemptCount() + 1;
        delivery.setAttemptCount(attempts);
        delivery.setLastError(truncate(errorMessage));

        if (attempts >= MAX_ATTEMPTS) {
            delivery.setStatus(DeliveryStatus.DEAD);
        } else {
            delivery.setStatus(DeliveryStatus.RETRYING);
            delivery.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds(attempts)));
        }

        int failures = endpoint.getConsecutiveFailures() + 1;
        endpoint.setConsecutiveFailures(failures);
        if (failures >= 10) {
            endpoint.setStatus(EndpointStatus.DISABLED);
        }
        endpointRepository.save(endpoint);
    }

    private void resetEndpointFailures(Endpoint endpoint) {
        if (endpoint.getConsecutiveFailures() != 0) {
            endpoint.setConsecutiveFailures(0);
            endpointRepository.save(endpoint);
        }
    }

    private long backoffSeconds(int attempt) {
        long base = (long) Math.pow(2, attempt);
        long jitter = (long) (Math.random() * 3);
        return Math.min(base + jitter, 300);
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > 500 ? s.substring(0, 500) : s;
    }
}