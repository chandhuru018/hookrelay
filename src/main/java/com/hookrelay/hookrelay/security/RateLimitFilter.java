package com.hookrelay.hookrelay.security;

import com.hookrelay.hookrelay.domain.Tenant;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String REGISTER_PATH = "/api/v1/tenants";
    private static final String API_PREFIX = "/api/v1/";

    private final boolean enabled;
    private final RateLimiter registerLimiter;
    private final RateLimiter apiLimiter;
    private final ClientIpResolver clientIpResolver;

    public RateLimitFilter(
            @Value("${hookrelay.rate-limit.enabled:true}") boolean enabled,
            @Value("${hookrelay.rate-limit.register.limit:5}") int registerLimit,
            @Value("${hookrelay.rate-limit.register.window-seconds:3600}") long registerWindowSeconds,
            @Value("${hookrelay.rate-limit.api.limit:300}") int apiLimit,
            @Value("${hookrelay.rate-limit.api.window-seconds:60}") long apiWindowSeconds,
            ClientIpResolver clientIpResolver) {
        this.enabled = enabled;
        this.registerLimiter = new RateLimiter(registerLimit, Duration.ofSeconds(registerWindowSeconds));
        this.apiLimiter = new RateLimiter(apiLimit, Duration.ofSeconds(apiWindowSeconds));
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (enabled) {
            String path = request.getRequestURI();
            RateLimiter.Decision decision = null;

            if ("POST".equalsIgnoreCase(request.getMethod()) && REGISTER_PATH.equals(path)) {
                decision = registerLimiter.tryAcquire("ip:" + clientIpResolver.resolve(request));
            } else if (path.startsWith(API_PREFIX)) {
                decision = apiLimiter.tryAcquire(apiKeyFor(request));
            }

            if (decision != null && !decision.allowed()) {
                reject(response, decision.retryAfterSeconds());
                return;
            }
        }

        chain.doFilter(request, response);
    }

    @Scheduled(fixedDelay = 300_000)
    public void evictIdleBuckets() {
        registerLimiter.evictIdle();
        apiLimiter.evictIdle();
    }

    private String apiKeyFor(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Tenant tenant) {
            return "tenant:" + tenant.getId();
        }
        return "ip:" + clientIpResolver.resolve(request);
    }

    private void reject(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Rate limit exceeded. Try again later.\"}");
    }
}