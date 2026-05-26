package com.aqar.auth.security;

import com.aqar.auth.UserEntity;
import com.aqar.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
	private final JwtProperties properties;
	private final Clock clock;
	private final SecretKey secretKey;

	public JwtService(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.secretKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
	}

	public String generateAccessToken(UserEntity user) {
		Instant now = clock.instant();
		Instant expiresAt = now.plus(properties.accessTokenTtl());
		return Jwts.builder()
				.setSubject(String.valueOf(user.getId()))
				.claim("role", user.getRole().name())
				.setIssuer(properties.issuer())
				.setIssuedAt(Date.from(now))
				.setExpiration(Date.from(expiresAt))
				.signWith(secretKey, SignatureAlgorithm.HS256)
				.compact();
	}

	public JwtPrincipal parseAccessToken(String token) {
		Jws<Claims> claims = Jwts.parserBuilder()
				.setClock(() -> Date.from(clock.instant()))
				.setSigningKey(secretKey)
				.requireIssuer(properties.issuer())
				.build()
				.parseClaimsJws(token);
			String subject = claims.getBody().getSubject();
			String role = claims.getBody().get("role", String.class);
			return new JwtPrincipal(Long.parseLong(subject), role);
	}

	public long accessTokenTtlSeconds() {
		Duration ttl = properties.accessTokenTtl();
		return ttl.toSeconds();
	}
}