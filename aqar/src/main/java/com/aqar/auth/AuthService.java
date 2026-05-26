package com.aqar.auth;

import com.aqar.auth.dto.AuthResponse;
import com.aqar.auth.dto.LoginRequest;
import com.aqar.auth.dto.LogoutRequest;
import com.aqar.auth.dto.RefreshRequest;
import com.aqar.auth.dto.RegisterRequest;
import com.aqar.auth.security.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {
	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final RefreshTokenService refreshTokenService;

	@Transactional
	public AuthResponse register(RegisterRequest request) {
		String email = normalizeEmail(request.email());
		if (userRepository.existsByEmail(email)) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
		}
		UserEntity user = new UserEntity(email, passwordEncoder.encode(request.password()), Role.USER);
		userRepository.save(user);
		return refreshTokenService.issueTokens(user);
	}

	@Transactional
	public AuthResponse login(LoginRequest request) {
		UserEntity user = userRepository.findByEmail(normalizeEmail(request.email()))
				.orElseThrow(() -> unauthorized("Invalid email or password"));
		if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
			throw unauthorized("Invalid email or password");
		}
		return refreshTokenService.issueTokens(user);
	}

	@Transactional
	public AuthResponse refresh(RefreshRequest request) {
		return refreshTokenService.refresh(request.refreshToken());
	}

	@Transactional
	public void logout(LogoutRequest request) {
		refreshTokenService.logout(request.refreshToken());
	}

	private ResponseStatusException unauthorized(String message) {
		return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
	}

	private String normalizeEmail(String email) {
		return email.trim().toLowerCase();
	}
}