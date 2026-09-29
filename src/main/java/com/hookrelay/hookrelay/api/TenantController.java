package com.hookrelay.hookrelay.api;

import com.hookrelay.hookrelay.service.TenantRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TenantController {

    private final TenantRegistrationService tenantRegistrationService;

    public TenantController(TenantRegistrationService tenantRegistrationService) {
        this.tenantRegistrationService = tenantRegistrationService;
    }

    @PostMapping("/api/v1/tenants")
    public ResponseEntity<TenantRegistrationResponse> register(
            @Valid @RequestBody TenantRegistrationRequest request) {

        var result = tenantRegistrationService.register(request.getName());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new TenantRegistrationResponse(result.tenantId(), result.rawApiKey()));
    }
}