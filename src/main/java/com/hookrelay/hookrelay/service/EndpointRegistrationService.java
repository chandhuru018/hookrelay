package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Endpoint;
import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.EndpointRepository;
import com.hookrelay.hookrelay.security.UrlSafetyValidator;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class EndpointRegistrationService {

    private final EndpointRepository endpointRepository;
    private final UrlSafetyValidator urlSafetyValidator;
    private final QuotaService quotaService;
    private final SecureRandom secureRandom = new SecureRandom();

    public EndpointRegistrationService(EndpointRepository endpointRepository,
                                       UrlSafetyValidator urlSafetyValidator,
                                       QuotaService quotaService) {
        this.endpointRepository = endpointRepository;
        this.urlSafetyValidator = urlSafetyValidator;
        this.quotaService = quotaService;
    }

    public Endpoint register(Tenant tenant, String url) {
        urlSafetyValidator.validate(url);
        quotaService.checkCanRegisterEndpoint(tenant.getId());

        String secret = generateSecret();
        Endpoint endpoint = new Endpoint(tenant, url.trim(), secret);
        return endpointRepository.save(endpoint);
    }

    private String generateSecret() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}