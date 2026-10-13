package com.hookrelay.hookrelay.security;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BodySizeLimitFilterTest {

    private final BodySizeLimitFilter filter = new BodySizeLimitFilter(100);

    @Test
    void rejectsDeclaredOversizedBodyWith413() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events");
        request.setContent(new byte[101]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(413, response.getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void passesBodyWithinLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events");
        request.setContent(new byte[100]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest());
    }

    @Test
    void stopsReadingUndeclaredBodyThatExceedsLimit() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/events") {
            @Override
            public long getContentLengthLong() {
                return -1;
            }

            @Override
            public int getContentLength() {
                return -1;
            }
        };
        request.setContent(new byte[500]);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain readsWholeBody = (req, res) -> req.getInputStream().readAllBytes();

        assertThrows(IOException.class, () -> filter.doFilter(request, response, readsWholeBody));
    }
}