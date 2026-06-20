package com.aqar.listing.config;

import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DropboxConfig {

    @Bean
    public DbxClientV2 dropboxClient(DropboxProperties props) {
        DbxRequestConfig config = DbxRequestConfig.newBuilder("aqar").build();
        return new DbxClientV2(config, props.accessToken());
    }
}