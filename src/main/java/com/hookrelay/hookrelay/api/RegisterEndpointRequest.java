package com.hookrelay.hookrelay.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterEndpointRequest {

    @NotBlank(message = "url is required")
    @Size(max = 500, message = "url must be at most 500 characters")
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}