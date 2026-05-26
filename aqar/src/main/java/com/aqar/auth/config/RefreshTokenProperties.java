package com.aqar.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.refresh-token")
public record RefreshTokenProperties(Duration ttl) {
}