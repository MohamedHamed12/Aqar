package com.aqar.user;

import com.aqar.user.security.JwtService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JwtServiceTest {

    @Test
    public void createAndVerifyToken() {
        JwtService svc = new JwtService("test-secret-123", 60);
        String token = svc.createAccessToken(42L, "ROLE_USER");
        var claims = svc.verify(token);
        Assertions.assertEquals(42L, claims.sub.longValue());
        Assertions.assertEquals("ROLE_USER", claims.role);
    }
}
