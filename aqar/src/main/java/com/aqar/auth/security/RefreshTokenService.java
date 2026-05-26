package com.aqar.auth.security;

import com.aqar.auth.RefreshTokenEntity;
import com.aqar.auth.RefreshTokenRepository;
import com.aqar.auth.UserEntity;
import com.aqar.auth.UserRepository;
import com.aqar.auth.config.RefreshTokenProperties;
import com.aqar.auth.dto.AuthResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {
	private static final int REFRESH_TOKEN_BYTE_LENGTH = 32;

	private final RefreshTokenRepository refreshTokenRepository;
	private final UserRepository userRepository;
	private final JwtService jwtService;
	private final RefreshTokenProperties properties;
	private final Clock clock;
	private final SecureRandom secureRandom = new SecureRandom();

	@Transactional
	public AuthResponse issueTokens(UserEntity user) {
		String refreshToken = generateRefreshTokenValue();
		String familyId = UUID.randomUUID().toString();
		RefreshTokenEntity tokenEntity = new RefreshTokenEntity(
				user,
				familyId,
				hash(refreshToken),
				clock.instant().plus(properties.ttl()));
		refreshTokenRepository.save(tokenEntity);
		return toResponse(user, jwtService.generateAccessToken(user), refreshToken);
	}

	@Transactional
	public AuthResponse refresh(String refreshTokenValue) {
		RefreshTokenEntity token = findToken(refreshTokenValue);
		Instant now = clock.instant();
		if (token.getRevokedAt() != null) {
			revokeFamily(token.getFamilyId(), now);
			throw unauthorized("Refresh token replay detected");
		}
		if (!token.getExpiresAt().isAfter(now)) {
			revokeFamily(token.getFamilyId(), now);
			throw unauthorized("Refresh token expired");
		}

		String nextRefreshToken = generateRefreshTokenValue();
		RefreshTokenEntity nextToken = new RefreshTokenEntity(
				token.getUser(),
				token.getFamilyId(),
				hash(nextRefreshToken),
				now.plus(properties.ttl()));
		refreshTokenRepository.save(nextToken);

		token.setRevokedAt(now);
		token.setReplacedByTokenId(nextToken.getId());
		refreshTokenRepository.save(token);

		return toResponse(token.getUser(), jwtService.generateAccessToken(token.getUser()), nextRefreshToken);
	}

	@Transactional
	public void logout(String refreshTokenValue) {
		RefreshTokenEntity token = findToken(refreshTokenValue);
		revokeFamily(token.getFamilyId(), clock.instant());
	}

	private RefreshTokenEntity findToken(String refreshTokenValue) {
		String tokenHash = hash(refreshTokenValue);
		return refreshTokenRepository.findByTokenHash(tokenHash)
				.orElseThrow(() -> unauthorized("Invalid refresh token"));
	}

	private void revokeFamily(String familyId, Instant revokedAt) {
		List<RefreshTokenEntity> tokens = refreshTokenRepository.findAllByFamilyId(familyId);
		for (RefreshTokenEntity token : tokens) {
			if (token.getRevokedAt() == null) {
				token.setRevokedAt(revokedAt);
				refreshTokenRepository.save(token);
			}
		}
	}

	private AuthResponse toResponse(UserEntity user, String accessToken, String refreshToken) {
		return new AuthResponse(
				accessToken,
				refreshToken,
				"Bearer",
				jwtService.accessTokenTtlSeconds(),
				properties.ttl().toSeconds(),
				user.getId(),
				user.getRole().name());
	}

	private String generateRefreshTokenValue() {
		byte[] bytes = new byte[REFRESH_TOKEN_BYTE_LENGTH];
		secureRandom.nextBytes(bytes);
		return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	private String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private ResponseStatusException unauthorized(String message) {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
	}
}