package com.aqar.user.service.impl;

import com.aqar.user.dto.AuthResponse;
import com.aqar.user.dto.LoginRequest;
import com.aqar.user.dto.RegisterRequest;
import com.aqar.user.entity.RefreshTokenEntity;
import com.aqar.user.entity.UserEntity;
import com.aqar.user.repository.RefreshTokenRepository;
import com.aqar.user.repository.UserRepository;
import com.aqar.user.security.JwtService;
import com.aqar.user.service.AuthService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthServiceImpl(UserRepository userRepository,
                           RefreshTokenRepository refreshTokenRepository,
                           BCryptPasswordEncoder passwordEncoder,
                           JwtService jwtService) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Override
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalStateException("email_exists");
        }
        String hash = passwordEncoder.encode(req.getPassword());
        UserEntity u = new UserEntity(req.getEmail(), hash);
        userRepository.save(u);
        String access = jwtService.createAccessToken(u.getId(), u.getRole());
        String refresh = createAndStoreRefreshToken(u);
        return new AuthResponse(access, refresh);
    }

    @Override
    public AuthResponse login(LoginRequest req) {
        Optional<UserEntity> ou = userRepository.findByEmail(req.getEmail());
        if (ou.isEmpty()) throw new IllegalStateException("invalid_credentials");
        UserEntity u = ou.get();
        if (!passwordEncoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new IllegalStateException("invalid_credentials");
        }
        String access = jwtService.createAccessToken(u.getId(), u.getRole());
        String refresh = createAndStoreRefreshToken(u);
        return new AuthResponse(access, refresh);
    }

    @Override
    public AuthResponse refresh(String refreshToken) {
        RefreshTokenEntity token = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new IllegalStateException("invalid_refresh"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(token);
            throw new IllegalStateException("expired_refresh");
        }
        UserEntity user = token.getUser();
        // rotate: delete old token
        refreshTokenRepository.delete(token);
        String newRefresh = createAndStoreRefreshToken(user);
        String access = jwtService.createAccessToken(user.getId(), user.getRole());
        return new AuthResponse(access, newRefresh);
    }

    @Override
    public void logout(String refreshToken) {
        refreshTokenRepository.findByToken(refreshToken).ifPresent(refreshTokenRepository::delete);
    }

    private String createAndStoreRefreshToken(UserEntity user) {
        String token = UUID.randomUUID().toString();
        Instant expires = Instant.now().plus(7, ChronoUnit.DAYS);
        RefreshTokenEntity entity = new RefreshTokenEntity(token, user, expires);
        refreshTokenRepository.save(entity);
        return token;
    }
}
