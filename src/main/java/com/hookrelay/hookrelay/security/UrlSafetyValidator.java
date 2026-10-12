package com.hookrelay.hookrelay.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

@Component
public class UrlSafetyValidator {

    private final boolean allowPrivateTargets;

    public UrlSafetyValidator(
            @Value("${hookrelay.security.allow-private-targets:false}") boolean allowPrivateTargets) {
        this.allowPrivateTargets = allowPrivateTargets;
    }

    public void validate(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new UnsafeUrlException("URL is required");
        }

        URI uri;
        try {
            uri = new URI(rawUrl.trim());
        } catch (URISyntaxException e) {
            throw new UnsafeUrlException("URL is not valid");
        }

        String scheme = uri.getScheme();
        if (scheme == null
                || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new UnsafeUrlException("Only http and https URLs are allowed");
        }

        if (uri.getUserInfo() != null) {
            throw new UnsafeUrlException("URLs containing credentials are not allowed");
        }

        String host = uri.getHost();
        if (host == null || host.isBlank()) {
            throw new UnsafeUrlException("URL must contain a host");
        }

        if (allowPrivateTargets) {
            return;
        }

        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new UnsafeUrlException("Host could not be resolved");
        }

        for (InetAddress address : addresses) {
            if (isInternal(address)) {
                throw new UnsafeUrlException("URL points to a private or internal address");
            }
        }
    }

    private boolean isInternal(InetAddress address) {
        if (address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }

        byte[] bytes = address.getAddress();

        if (bytes.length == 4) {
            int first = bytes[0] & 0xFF;
            int second = bytes[1] & 0xFF;
            int third = bytes[2] & 0xFF;

            if (first == 0) return true;                                   // 0.0.0.0/8
            if (first == 100 && second >= 64 && second <= 127) return true; // 100.64.0.0/10 (carrier-grade NAT)
            if (first == 192 && second == 0 && third == 0) return true;     // 192.0.0.0/24
            if (first == 198 && (second == 18 || second == 19)) return true; // 198.18.0.0/15
            if (first >= 240) return true;                                  // reserved and broadcast
        } else if (bytes.length == 16) {
            if ((bytes[0] & 0xFE) == 0xFC) return true;                     // fc00::/7 (unique local)
        }

        return false;
    }
}