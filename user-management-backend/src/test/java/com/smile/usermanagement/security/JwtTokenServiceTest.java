package com.smile.usermanagement.security;

import com.smile.usermanagement.config.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenServiceTest {

    @Test
    void issueAndVerifyToken() {
        JwtProperties props = new JwtProperties("unit-test-secret", "unit-test-issuer", 5);
        JwtTokenService service = new JwtTokenService(props);

        String token = service.issueToken(123L, "alice");
        assertNotNull(token);

        var decoded = service.verify(token);
        assertEquals("unit-test-issuer", decoded.getIssuer());
        assertEquals(123L, decoded.getClaim("uid").asLong());
        assertEquals("alice", decoded.getClaim("username").asString());
    }
}

