package com.hookrelay.hookrelay.api;

public class RegisterEndpointResponse {

    private final Long endpointId;
    private final String secret;

    public RegisterEndpointResponse(Long endpointId, String secret) {
        this.endpointId = endpointId;
        this.secret = secret;
    }

    public Long getEndpointId() { return endpointId; }
    public String getSecret() { return secret; }
}