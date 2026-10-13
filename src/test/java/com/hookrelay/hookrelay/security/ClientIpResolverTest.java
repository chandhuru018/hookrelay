package com.hookrelay.hookrelay.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientIpResolverTest {

    private MockHttpServletRequest request(String remoteAddr, String... forwardedHeaders) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        for (String header : forwardedHeaders) {
            request.addHeader("X-Forwarded-For", header);
        }
        return request;
    }

    @Test
    void ignoresForwardedHeaderWhenNoProxyIsTrusted() {
        ClientIpResolver resolver = new ClientIpResolver(0);

        assertEquals("10.0.0.1", resolver.resolve(request("10.0.0.1", "9.9.9.9")));
    }

    @Test
    void usesRightmostEntryWithOneTrustedProxy() {
        ClientIpResolver resolver = new ClientIpResolver(1);

        assertEquals("203.0.113.5", resolver.resolve(request("10.0.0.1", "203.0.113.5")));
    }

    @Test
    void ignoresEntriesTheClientAddedItself() {
        ClientIpResolver resolver = new ClientIpResolver(1);

        assertEquals("203.0.113.5", resolver.resolve(request("10.0.0.1", "9.9.9.9, 203.0.113.5")));
    }

    @Test
    void skipsTwoTrustedProxiesFromTheRight() {
        ClientIpResolver resolver = new ClientIpResolver(2);

        assertEquals("203.0.113.5",
                resolver.resolve(request("10.0.0.1", "203.0.113.5, 198.51.100.7")));
    }

    @Test
    void combinesRepeatedHeaderLines() {
        ClientIpResolver resolver = new ClientIpResolver(1);

        assertEquals("203.0.113.5",
                resolver.resolve(request("10.0.0.1", "9.9.9.9", "203.0.113.5")));
    }

    @Test
    void fallsBackToPeerAddressWhenChainIsShorterThanTrustedProxies() {
        ClientIpResolver resolver = new ClientIpResolver(2);

        assertEquals("10.0.0.1", resolver.resolve(request("10.0.0.1", "203.0.113.5")));
    }

    @Test
    void fallsBackToPeerAddressWhenHeaderIsMissing() {
        ClientIpResolver resolver = new ClientIpResolver(1);

        assertEquals("10.0.0.1", resolver.resolve(request("10.0.0.1")));
    }
}