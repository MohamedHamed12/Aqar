package com.aqar.auth.security;

public record JwtPrincipal(long userId, String role) {
}