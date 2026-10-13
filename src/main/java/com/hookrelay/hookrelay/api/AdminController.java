package com.hookrelay.hookrelay.api;

import com.hookrelay.hookrelay.security.ClientIpResolver;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@Hidden
@RestController
public class AdminController {

    private final ClientIpResolver clientIpResolver;

    public AdminController(ClientIpResolver clientIpResolver) {
        this.clientIpResolver = clientIpResolver;
    }

    @GetMapping("/admin/client-info")
    public Map<String, Object> clientInfo(HttpServletRequest request) {
        return Map.of(
                "remoteAddr", request.getRemoteAddr(),
                "xForwardedFor", Collections.list(request.getHeaders("X-Forwarded-For")),
                "resolvedClientIp", clientIpResolver.resolve(request));
    }
}