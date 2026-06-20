package com.aqar.listing.config;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
public class DropboxConfig {

    @Bean
    @Lazy
    public DbxClientV2 dropboxClient(DropboxProperties props) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("aqar").build();
        return new DbxClientV2(config, props.accessToken());
    }
}