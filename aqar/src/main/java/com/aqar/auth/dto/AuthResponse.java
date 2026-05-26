package com.aqar.auth.dto;

public record AuthResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long accessTokenExpiresInSeconds,
		long refreshTokenExpiresInSeconds,
		long userId,
		String role) {
}