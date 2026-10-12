package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Delivery;
import com.hookrelay.hookrelay.domain.DeliveryStatus;
import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.EndpointStatus;
import com.hookrelay.hookrelay.domain.Event;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import com.hookrelay.hookrelay.repository.EndpointRepository;
import com.hookrelay.hookrelay.security.UnsafeUrlException;
import com.hookrelay.hookrelay.security.UrlSafetyValidator;
import com.hookrelay.hookrelay.security.WebhookSigner;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class DeliveryExecutionService {

    private static final int MAX_ATTEMPTS = 6;

    private final DeliveryRepository deliveryRepository;
    private final EndpointRepository endpointRepository;
    private final WebhookSigner webhookSigner;
    private final MeterRegistry meterRegistry;
    private final UrlSafetyValidator urlSafetyValidator;
    private final RestClient restClient;

    public DeliveryExecutionService(DeliveryRepository deliveryRepository,
                                    EndpointRepository endpointRepository,
                                    WebhookSigner webhookSigner,
                                    MeterRegistry meterRegistry,
                                    UrlSafetyValidator urlSafetyValidator) {
        this.deliveryRepository = deliveryRepository;
        this.endpointRepository = endpointRepository;
        this.webhookSigner = webhookSigner;
        this.meterRegistry = meterRegistry;
        this.urlSafetyValidator = urlSafetyValidator;

        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(10));
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
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

        try {
            urlSafetyValidator.validate(endpoint.getUrl());
        } catch (UnsafeUrlException e) {
            blockDelivery(delivery, endpoint, e.getMessage());
            deliveryRepository.save(delivery);
            return;
        }

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
                    .onStatus(status -> !status.is2xxSuccessful(), (request, response) -> {
                        throw new IllegalStateException(
                                "Endpoint responded with HTTP " + response.getStatusCode().value());
                    })
                    .toBodilessEntity();

            delivery.setStatus(DeliveryStatus.DELIVERED);
            meterRegistry.counter("hookrelay.deliveries", "outcome", "delivered").increment();
            resetEndpointFailures(endpoint);

        } catch (Exception e) {
            handleFailure(delivery, endpoint, e.getMessage());
        }

        deliveryRepository.save(delivery);
    }

    private void blockDelivery(Delivery delivery, Endpoint endpoint, String reason) {
        delivery.setStatus(DeliveryStatus.DEAD);
        delivery.setAttemptCount(delivery.getAttemptCount() + 1);
        delivery.setLastError(truncate("Blocked: " + reason));

        endpoint.setStatus(EndpointStatus.DISABLED);
        endpointRepository.save(endpoint);

        meterRegistry.counter("hookrelay.deliveries", "outcome", "blocked").increment();
    }

    private void handleFailure(Delivery delivery, Endpoint endpoint, String errorMessage) {
        int attempts = delivery.getAttemptCount() + 1;
        delivery.setAttemptCount(attempts);
        delivery.setLastError(truncate(errorMessage));

        if (attempts >= MAX_ATTEMPTS) {
            delivery.setStatus(DeliveryStatus.DEAD);
            meterRegistry.counter("hookrelay.deliveries", "outcome", "dead").increment();
        } else {
            delivery.setStatus(DeliveryStatus.RETRYING);
            delivery.setNextAttemptAt(Instant.now().plusSeconds(backoffSeconds(attempts)));
            meterRegistry.counter("hookrelay.deliveries", "outcome", "retry").increment();
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