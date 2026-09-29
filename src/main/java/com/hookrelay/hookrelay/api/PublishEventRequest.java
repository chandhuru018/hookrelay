package com.hookrelay.hookrelay.api;

import jakarta.validation.constraints.NotBlank;

public class PublishEventRequest {

    @NotBlank(message = "type is required")
    private String type;

    @NotBlank(message = "payload is required")
    private String payload;

    @NotBlank(message = "idempotencyKey is required")
    private String idempotencyKey;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}