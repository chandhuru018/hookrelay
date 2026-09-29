package com.hookrelay.hookrelay.api;

import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.service.DeliveryStatsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
public class StatsController {

    private final DeliveryStatsService deliveryStatsService;

    public StatsController(DeliveryStatsService deliveryStatsService) {
        this.deliveryStatsService = deliveryStatsService;
    }

    @GetMapping("/api/v1/stats")
    public DeliveryStatsResponse getStats(Principal principal) {
        Tenant tenant = (Tenant) ((UsernamePasswordAuthenticationToken) principal).getPrincipal();
        return deliveryStatsService.getStats(tenant.getId());
    }
}