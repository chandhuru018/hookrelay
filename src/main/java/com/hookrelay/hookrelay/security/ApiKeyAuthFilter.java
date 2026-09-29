package com.hookrelay.hookrelay.security;

import com.hookrelay.hookrelay.domain.Tenant;
import com.hookrelay.hookrelay.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;
    private final ApiKeyHasher apiKeyHasher;

    public ApiKeyAuthFilter(TenantRepository tenantRepository, ApiKeyHasher apiKeyHasher) {
        this.tenantRepository = tenantRepository;
        this.apiKeyHasher = apiKeyHasher;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");

        if (apiKey != null && !apiKey.isBlank()) {
            String hash = apiKeyHasher.hash(apiKey);
            Optional<Tenant> tenant = tenantRepository.findByApiKeyHash(hash);

            if (tenant.isPresent()) {
                var authentication = new UsernamePasswordAuthenticationToken(
                        tenant.get(), null, List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}