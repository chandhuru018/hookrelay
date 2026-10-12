package com.hookrelay.hookrelay.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PublishEventRequest {

    @NotBlank(message = "type is required")
    @Size(max = 100, message = "type must be at most 100 characters")
    private String type;

    @NotBlank(message = "payload is required")
    @Size(max = 65536, message = "payload must be at most 65536 characters")
    private String payload;

    @NotBlank(message = "idempotencyKey is required")
    @Size(max = 100, message = "idempotencyKey must be at most 100 characters")
    private String idempotencyKey;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
}