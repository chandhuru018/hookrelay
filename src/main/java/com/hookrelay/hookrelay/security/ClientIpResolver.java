package com.hookrelay.hookrelay.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Component
public class ClientIpResolver {

    private final int trustedProxies;

    public ClientIpResolver(@Value("${hookrelay.proxy.trusted-hops:0}") int trustedProxies) {
        this.trustedProxies = trustedProxies;
    }

    public String resolve(HttpServletRequest request) {
        String peerAddress = request.getRemoteAddr();
        if (trustedProxies <= 0) {
            return peerAddress;
        }

        List<String> forwarded = Collections.list(request.getHeaders("X-Forwarded-For")).stream()
                .flatMap(header -> Arrays.stream(header.split(",")))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();

        int index = forwarded.size() - trustedProxies;
        if (index < 0) {
            return peerAddress;
        }
        return forwarded.get(index);
    }
}