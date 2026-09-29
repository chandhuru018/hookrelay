package com.hookrelay.hookrelay.api;

import java.util.Map;

public class DeliveryStatsResponse {

    private final long totalDeliveries;
    private final Map<String, Long> countByStatus;
    private final double successRatePercent;

    public DeliveryStatsResponse(long totalDeliveries, Map<String, Long> countByStatus, double successRatePercent) {
        this.totalDeliveries = totalDeliveries;
        this.countByStatus = countByStatus;
        this.successRatePercent = successRatePercent;
    }

    public long getTotalDeliveries() { return totalDeliveries; }
    public Map<String, Long> getCountByStatus() { return countByStatus; }
    public double getSuccessRatePercent() { return successRatePercent; }
}