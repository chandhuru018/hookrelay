package com.hookrelay.hookrelay.api;

public class TenantRegistrationResponse {

    private final Long tenantId;
    private final String apiKey;

    public TenantRegistrationResponse(Long tenantId, String apiKey) {
        this.tenantId = tenantId;
        this.apiKey = apiKey;
    }

    public Long getTenantId() { return tenantId; }
    public String getApiKey() { return apiKey; }
}