package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.EndpointRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EndpointRegistrationService {

    private final EndpointRepository endpointRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public EndpointRegistrationService(EndpointRepository endpointRepository) {
        this.endpointRepository = endpointRepository;
    }

    public Endpoint register(Tenant tenant, String url) {
        String secret = generateSecret();
        Endpoint endpoint = new Endpoint(tenant, url, secret);
        return endpointRepository.save(endpoint);
    }

    private String generateSecret() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}