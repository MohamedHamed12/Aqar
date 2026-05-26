package com.aqar.user.service;

import com.aqar.user.dto.AuthResponse;
import com.aqar.user.dto.LoginRequest;
import com.aqar.user.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest req);
    AuthResponse login(LoginRequest req);
    AuthResponse refresh(String refreshToken);
    void logout(String refreshToken);
}
