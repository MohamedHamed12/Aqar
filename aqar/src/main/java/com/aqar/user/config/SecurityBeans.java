package com.aqar.user.config;

import com.aqar.user.security.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class SecurityBeans {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtService jwtService() {
        String secret = System.getenv().getOrDefault("AQAR_JWT_SECRET", "change-this-secret-in-prod");
        long accessSeconds = 15 * 60; // 15 minutes
        return new JwtService(secret, accessSeconds);
    }
}
