package com.hookrelay.hookrelay.api;

import com.hookrelay.hookrelay.domain.Delivery;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.service.DeliveryReplayService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
public class DeliveryController {

    private final DeliveryReplayService deliveryReplayService;

    public DeliveryController(DeliveryReplayService deliveryReplayService) {
        this.deliveryReplayService = deliveryReplayService;
    }

    @PostMapping("/api/v1/deliveries/{id}/replay")
    public ResponseEntity<Map<String, Object>> replay(@PathVariable Long id, Principal principal) {
        Tenant tenant = (Tenant) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();

        Delivery delivery = deliveryReplayService.replay(tenant, id);

        return ResponseEntity.ok(Map.of(
                "deliveryId", delivery.getId(),
                "status", delivery.getStatus().name()));
    }
}