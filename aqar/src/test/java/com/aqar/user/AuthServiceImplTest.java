package com.aqar.user;

import com.aqar.user.dto.LoginRequest;
import com.aqar.user.dto.RegisterRequest;
import com.aqar.user.entity.RefreshTokenEntity;
import com.aqar.user.entity.UserEntity;
import com.aqar.user.repository.RefreshTokenRepository;
import com.aqar.user.repository.UserRepository;
import com.aqar.user.security.JwtService;
import com.aqar.user.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private BCryptPasswordEncoder passwordEncoder;
    private JwtService jwtService;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        jwtService = new JwtService("test-secret-123", 60);
        authService = new AuthServiceImpl(userRepository, refreshTokenRepository, passwordEncoder, jwtService);
    }

    @Test
    void registerCreatesUserAndTokens() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("agent@aqar.test");
        request.setPassword("secret123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> {
            UserEntity user = invocation.getArgument(0);
            user.setId(7L);
            return user;
        });
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.register(request);

        Assertions.assertNotNull(response.getAccessToken());
        Assertions.assertNotNull(response.getRefreshToken());

        var claims = jwtService.verify(response.getAccessToken());
        Assertions.assertEquals(7L, claims.sub.longValue());
        Assertions.assertEquals("USER", claims.role);

        ArgumentCaptor<UserEntity> userCaptor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepository).save(userCaptor.capture());
        Assertions.assertEquals("agent@aqar.test", userCaptor.getValue().getEmail());
        Assertions.assertTrue(passwordEncoder.matches("secret123", userCaptor.getValue().getPasswordHash()));
    }

    @Test
    void registerRejectsDuplicateEmail() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("agent@aqar.test");
        request.setPassword("secret123");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        Assertions.assertThrows(IllegalStateException.class, () -> authService.register(request));
        verify(userRepository).existsByEmail("agent@aqar.test");
    }

    @Test
    void loginValidatesPasswordAndIssuesTokens() {
        UserEntity user = new UserEntity();
        user.setId(11L);
        user.setEmail("agent@aqar.test");
        user.setPasswordHash(passwordEncoder.encode("secret123"));

        LoginRequest request = new LoginRequest();
        request.setEmail("agent@aqar.test");
        request.setPassword("secret123");

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.login(request);

        Assertions.assertNotNull(response.getAccessToken());
        Assertions.assertNotNull(response.getRefreshToken());
        Assertions.assertEquals(11L, jwtService.verify(response.getAccessToken()).sub.longValue());
    }

    @Test
    void refreshRotatesRefreshToken() {
        UserEntity user = new UserEntity();
        user.setId(12L);
        user.setRole("AGENT");

        RefreshTokenEntity storedToken = new RefreshTokenEntity("refresh-1", user, Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("refresh-1")).thenReturn(Optional.of(storedToken));
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = authService.refresh("refresh-1");

        verify(refreshTokenRepository).delete(storedToken);
        Assertions.assertNotNull(response.getAccessToken());
        Assertions.assertNotNull(response.getRefreshToken());
        Assertions.assertNotEquals("refresh-1", response.getRefreshToken());
        Assertions.assertEquals(12L, jwtService.verify(response.getAccessToken()).sub.longValue());
    }

    @Test
    void logoutDeletesStoredTokenWhenFound() {
        UserEntity user = new UserEntity();
        RefreshTokenEntity storedToken = new RefreshTokenEntity("refresh-1", user, Instant.now().plusSeconds(3600));

        when(refreshTokenRepository.findByToken("refresh-1")).thenReturn(Optional.of(storedToken));

        authService.logout("refresh-1");

        verify(refreshTokenRepository).delete(storedToken);
    }
}