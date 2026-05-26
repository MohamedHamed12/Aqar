package com.aqar.auth;

import com.aqar.auth.config.JwtProperties;
import com.aqar.auth.security.JwtPrincipal;
import com.aqar.auth.security.JwtService;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

	@Test
	void generatesTokenWithExpectedClaims() {
		Instant now = Instant.parse("2026-05-18T00:00:00Z");
		JwtService service = new JwtService(
				new JwtProperties("aqar-test", "a-long-test-secret-value-a-long-test-secret-value", Duration.ofMinutes(15)),
				Clock.fixed(now, ZoneOffset.UTC));

		UserEntity user = new UserEntity("user@example.com", "hash", Role.USER);
		user.setId(42L);

		String token = service.generateAccessToken(user);
		JwtPrincipal principal = service.parseAccessToken(token);

		assertEquals(42L, principal.userId());
		assertEquals("USER", principal.role());
	}

	@Test
	void rejectsTamperedSignature() {
		Instant now = Instant.parse("2026-05-18T00:00:00Z");
		JwtService service = new JwtService(
				new JwtProperties("aqar-test", "a-long-test-secret-value-a-long-test-secret-value", Duration.ofMinutes(15)),
				Clock.fixed(now, ZoneOffset.UTC));

		UserEntity user = new UserEntity("user@example.com", "hash", Role.USER);
		user.setId(42L);

		String token = service.generateAccessToken(user);
		String tamperedToken = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

		assertThrows(JwtException.class, () -> service.parseAccessToken(tamperedToken));
	}

	@Test
	void rejectsExpiredToken() {
		Instant now = Instant.parse("2026-05-18T00:00:00Z");
		JwtProperties properties = new JwtProperties(
				"aqar-test",
				"a-long-test-secret-value-a-long-test-secret-value",
				Duration.ofSeconds(1));
		JwtService issuingService = new JwtService(properties, Clock.fixed(now, ZoneOffset.UTC));
		JwtService validatingService = new JwtService(properties, Clock.fixed(now.plusSeconds(2), ZoneOffset.UTC));

		UserEntity user = new UserEntity("user@example.com", "hash", Role.USER);
		user.setId(42L);

		String token = issuingService.generateAccessToken(user);
		assertThrows(JwtException.class, () -> validatingService.parseAccessToken(token));
	}
}