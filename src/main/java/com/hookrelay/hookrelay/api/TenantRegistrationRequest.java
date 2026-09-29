package com.hookrelay.hookrelay.api;

import jakarta.validation.constraints.NotBlank;

public class TenantRegistrationRequest {

    @NotBlank(message = "name is required")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}