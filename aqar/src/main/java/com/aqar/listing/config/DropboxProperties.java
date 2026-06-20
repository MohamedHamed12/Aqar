package com.aqar.listing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.dropbox")
public record DropboxProperties(
        String accessToken,
        String appFolder,
        Duration sharedLinkTtl
) {}
