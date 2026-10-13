package com.hookrelay.hookrelay.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "hookrelay.worker.enabled=false",
                "hookrelay.admin.password=test-admin-password"
        })
class SecurityIntegrationTest {

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Value("${local.server.port}")
    private int port;

    private final HttpClient client = HttpClient.newHttpClient();

    private int get(String path, String... headers) throws Exception {
        HttpRequest.Builder builder = HttpRequest
                .newBuilder(URI.create("http://localhost:" + port + path))
                .GET();
        if (headers.length > 0) {
            builder.headers(headers);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private String basic(String user, String password) {
        String raw = user + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void healthIsPublic() throws Exception {
        assertEquals(200, get("/actuator/health"));
    }

    @Test
    void livenessProbeIsPublic() throws Exception {
        assertEquals(200, get("/actuator/health/liveness"));
    }

    @Test
    void metricsRequireAdminCredentials() throws Exception {
        assertEquals(401, get("/actuator/metrics"));
        assertEquals(401, get("/actuator/metrics", "Authorization", basic("admin", "wrong-password")));
        assertEquals(200, get("/actuator/metrics", "Authorization", basic("admin", "test-admin-password")));
    }

    @Test
    void apiDocsArePublic() throws Exception {
        assertEquals(200, get("/v3/api-docs"));
    }

    @Test
    void apiRejectsMissingOrInvalidKeyWith401() throws Exception {
        assertEquals(401, get("/api/v1/stats"));
        assertEquals(401, get("/api/v1/stats", "X-API-Key", "hrk_not-a-real-key"));
    }
}