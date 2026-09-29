package com.hookrelay.hookrelay.api;

import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.service.EndpointRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class EndpointController {

    private final EndpointRegistrationService endpointRegistrationService;

    public EndpointController(EndpointRegistrationService endpointRegistrationService) {
        this.endpointRegistrationService = endpointRegistrationService;
    }

    @PostMapping("/api/v1/endpoints")
    public ResponseEntity<RegisterEndpointResponse> register(
            @Valid @RequestBody RegisterEndpointRequest request,
            Principal principal) {

        Tenant tenant = (Tenant) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();

        Endpoint endpoint = endpointRegistrationService.register(tenant, request.getUrl());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegisterEndpointResponse(endpoint.getId(), endpoint.getSecret()));
    }
}