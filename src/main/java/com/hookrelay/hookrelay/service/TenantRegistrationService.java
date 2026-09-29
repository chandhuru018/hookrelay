package com.hookrelay.hookrelay.service;

import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.TenantRepository;
import com.hookrelay.hookrelay.security.ApiKeyHasher;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Base64;

@Service
public class TenantRegistrationService {

    private final TenantRepository tenantRepository;
    private final ApiKeyHasher apiKeyHasher;
    private final SecureRandom secureRandom = new SecureRandom();

    public TenantRegistrationService(TenantRepository tenantRepository, ApiKeyHasher apiKeyHasher) {
        this.tenantRepository = tenantRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    public TenantRegistrationResult register(String name) {
        String rawApiKey = generateApiKey();
        String hash = apiKeyHasher.hash(rawApiKey);

        Tenant tenant = new Tenant(name, hash);
        tenant = tenantRepository.save(tenant);

        return new TenantRegistrationResult(tenant.getId(), rawApiKey);
    }

    private String generateApiKey() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return "hrk_" + Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public record TenantRegistrationResult(Long tenantId, String rawApiKey) {
    }
}