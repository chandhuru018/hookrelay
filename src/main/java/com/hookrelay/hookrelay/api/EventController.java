package com.hookrelay.hookrelay.api;

import com.hookrelay.hookrelay.domain.Event;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.service.EventPublishingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class EventController {

    private final EventPublishingService eventPublishingService;

    public EventController(EventPublishingService eventPublishingService) {
        this.eventPublishingService = eventPublishingService;
    }

    @PostMapping("/api/v1/events")
    public ResponseEntity<PublishEventResponse> publishEvent(
            @Valid @RequestBody PublishEventRequest request,
            Principal principal) {

        Tenant tenant = (Tenant) ((org.springframework.security.authentication.UsernamePasswordAuthenticationToken) principal)
                .getPrincipal();

        Event event = eventPublishingService.publishEvent(
                tenant, request.getType(), request.getPayload(), request.getIdempotencyKey());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new PublishEventResponse(event.getId(), "ACCEPTED"));
    }
}