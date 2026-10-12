package com.hookrelay.hookrelay.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UrlSafetyValidatorTest {

    private final UrlSafetyValidator strict = new UrlSafetyValidator(false);

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1:8080/hook",
            "http://localhost/hook",
            "http://[::1]/hook",
            "http://169.254.169.254/latest/meta-data",
            "http://10.0.0.5/hook",
            "http://172.16.0.1/hook",
            "http://192.168.1.10/hook",
            "http://0.0.0.0/hook",
            "http://100.64.0.1/hook",
            "http://2130706433/hook"
    })
    void rejectsInternalAddresses(String url) {
        assertThrows(UnsafeUrlException.class, () -> strict.validate(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd",
            "ftp://8.8.8.8/file",
            "gopher://8.8.8.8/",
            "http://user:pass@8.8.8.8/hook",
            "not a url",
            "http:///no-host",
            ""
    })
    void rejectsUnsupportedOrMalformedUrls(String url) {
        assertThrows(UnsafeUrlException.class, () -> strict.validate(url));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://8.8.8.8/hook",
            "https://8.8.8.8:8443/hook"
    })
    void acceptsPublicAddresses(String url) {
        assertDoesNotThrow(() -> strict.validate(url));
    }

    @Test
    void allowsPrivateTargetsWhenExplicitlyEnabled() {
        UrlSafetyValidator relaxed = new UrlSafetyValidator(true);
        assertDoesNotThrow(() -> relaxed.validate("http://127.0.0.1:9090/"));
    }

    @Test
    void stillRejectsBadSchemesWhenPrivateTargetsAllowed() {
        UrlSafetyValidator relaxed = new UrlSafetyValidator(true);
        assertThrows(UnsafeUrlException.class, () -> relaxed.validate("file:///etc/passwd"));
    }
}