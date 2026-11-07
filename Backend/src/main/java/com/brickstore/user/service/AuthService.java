package com.brickstore.user.service;

import com.brickstore.user.dto.request.LoginRequest;
import com.brickstore.user.dto.request.RefreshTokenRequest;
import com.brickstore.user.dto.request.RegisterRequest;
import com.brickstore.user.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(String refreshToken);
}

