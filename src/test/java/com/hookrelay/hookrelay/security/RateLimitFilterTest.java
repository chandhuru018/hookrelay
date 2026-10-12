package com.hookrelay.hookrelay.security;

import com.hookrelay.hookrelay.domain.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class RateLimitFilterTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletResponse send(RateLimitFilter filter, String method, String path, String remoteAddr)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(remoteAddr);
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, new MockFilterChain());
        return response;
    }

    private void authenticateAs(long tenantId) {
        Tenant tenant = new Tenant("Tenant " + tenantId, "hash-" + tenantId);
        ReflectionTestUtils.setField(tenant, "id", tenantId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(tenant, null, List.of()));
    }

    @Test
    void limitsRegistrationPerIp() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 2, 3600, 300, 60);

        assertEquals(200, send(filter, "POST", "/api/v1/tenants", "1.2.3.4").getStatus());
        assertEquals(200, send(filter, "POST", "/api/v1/tenants", "1.2.3.4").getStatus());

        MockHttpServletResponse blocked = send(filter, "POST", "/api/v1/tenants", "1.2.3.4");
        assertEquals(429, blocked.getStatus());
        assertNotNull(blocked.getHeader("Retry-After"));

        assertEquals(200, send(filter, "POST", "/api/v1/tenants", "5.6.7.8").getStatus());
    }

    @Test
    void limitsApiRequestsPerTenant() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 5, 3600, 2, 60);

        authenticateAs(1L);
        assertEquals(200, send(filter, "POST", "/api/v1/events", "1.2.3.4").getStatus());
        assertEquals(200, send(filter, "POST", "/api/v1/events", "1.2.3.4").getStatus());
        assertEquals(429, send(filter, "POST", "/api/v1/events", "1.2.3.4").getStatus());

        authenticateAs(2L);
        assertEquals(200, send(filter, "POST", "/api/v1/events", "1.2.3.4").getStatus());
    }

    @Test
    void neverLimitsNonApiPaths() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(true, 1, 3600, 1, 60);

        for (int i = 0; i < 5; i++) {
            assertEquals(200, send(filter, "GET", "/actuator/health", "1.2.3.4").getStatus());
            assertEquals(200, send(filter, "GET", "/swagger-ui/index.html", "1.2.3.4").getStatus());
        }
    }

    @Test
    void passesEverythingWhenDisabled() throws Exception {
        RateLimitFilter filter = new RateLimitFilter(false, 1, 3600, 1, 60);

        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = send(filter, "POST", "/api/v1/tenants", "1.2.3.4");
            assertEquals(200, response.getStatus());
            assertNull(response.getHeader("Retry-After"));
        }
    }
}