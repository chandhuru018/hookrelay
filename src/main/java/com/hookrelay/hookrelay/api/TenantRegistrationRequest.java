package com.hookrelay.hookrelay.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TenantRegistrationRequest {

    @NotBlank(message = "name is required")
    @Size(max = 100, message = "name must be at most 100 characters")
    private String name;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}