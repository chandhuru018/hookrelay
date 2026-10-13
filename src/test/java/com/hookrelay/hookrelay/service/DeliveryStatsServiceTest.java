package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.api.DeliveryStatsResponse;
import com.hookrelay.hookrelay.domain.DeliveryStatus;
import com.hookrelay.hookrelay.repository.DeliveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryStatsServiceTest {

    @Mock
    private DeliveryRepository deliveryRepository;

    @InjectMocks
    private DeliveryStatsService deliveryStatsService;

    @Test
    void computesTotalsAndSuccessRateFromGroupedCounts() {
        when(deliveryRepository.countByStatusForTenant(1L)).thenReturn(List.of(
                new Object[]{DeliveryStatus.DELIVERED, 3L},
                new Object[]{DeliveryStatus.DEAD, 1L}));

        DeliveryStatsResponse response = deliveryStatsService.getStats(1L);

        assertEquals(4L, response.getTotalDeliveries());
        assertEquals(3L, (long) response.getCountByStatus().get("DELIVERED"));
        assertEquals(1L, (long) response.getCountByStatus().get("DEAD"));
        assertEquals(75.0, response.getSuccessRatePercent(), 0.0001);
    }

    @Test
    void returnsZerosWhenTenantHasNoDeliveries() {
        when(deliveryRepository.countByStatusForTenant(2L)).thenReturn(List.of());

        DeliveryStatsResponse response = deliveryStatsService.getStats(2L);

        assertEquals(0L, response.getTotalDeliveries());
        assertEquals(0.0, response.getSuccessRatePercent(), 0.0001);
        assertTrue(response.getCountByStatus().isEmpty());
    }
}