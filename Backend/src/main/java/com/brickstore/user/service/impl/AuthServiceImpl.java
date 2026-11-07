package com.brickstore.user.service.impl;

import com.brickstore.common.enums.ErrorCode;
import com.brickstore.common.exception.BusinessException;
import com.brickstore.security.CustomUserDetails;
import com.brickstore.security.jwt.JwtTokenProvider;
import com.brickstore.user.dto.request.LoginRequest;
import com.brickstore.user.dto.request.RefreshTokenRequest;
import com.brickstore.user.dto.request.RegisterRequest;
import com.brickstore.user.dto.response.AuthResponse;
import com.brickstore.user.entity.RefreshToken;
import com.brickstore.user.entity.Role;
import com.brickstore.user.entity.User;
import com.brickstore.user.mapper.UserMapper;
import com.brickstore.user.repository.RefreshTokenRepository;
import com.brickstore.user.repository.RoleRepository;
import com.brickstore.user.repository.UserRepository;
import com.brickstore.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().trim().toLowerCase())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS, "Email '" + request.getEmail() + "' đã được sử dụng");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseGet(() -> roleRepository.save(Role.builder()
                        .name("ROLE_USER")
                        .description("Người dùng thông thường, mua sắm trên hệ thống")
                        .build()));

        User user = User.builder()
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName().trim())
                .phone(request.getPhone())
                .provider("LOCAL")
                .isActive(true)
                .roles(Collections.singleton(userRole))
                .build();

        User savedUser = userRepository.save(user);
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);

        String accessToken = tokenProvider.generateAccessToken(userDetails);
        String refreshTokenString = tokenProvider.generateRefreshToken(savedUser.getEmail());

        saveRefreshToken(savedUser, refreshTokenString);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessExpirationMs() / 1000)
                .user(userMapper.toResponse(savedUser))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );

            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            User user = userDetails.getUser();

            if (user.getIsActive() != null && !user.getIsActive()) {
                throw new BusinessException(ErrorCode.ACCESS_DENIED, "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ ban quản trị.");
            }

            String accessToken = tokenProvider.generateAccessToken(userDetails);
            String refreshTokenString = tokenProvider.generateRefreshToken(user.getEmail());

            saveRefreshToken(user, refreshTokenString);

            return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshTokenString)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessExpirationMs() / 1000)
                .user(userMapper.toResponse(user))
                .build();
        } catch (BadCredentialsException ex) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS, "Email hoặc mật khẩu không chính xác");
        }
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshToken = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token không tồn tại hoặc không hợp lệ"));

        if (refreshToken.getRevoked() != null && refreshToken.getRevoked()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Refresh token này đã bị thu hồi. Vui lòng đăng nhập lại.");
        }

        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshToken);
            throw new BusinessException(ErrorCode.TOKEN_EXPIRED, "Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        if (!tokenProvider.validateToken(requestRefreshToken)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Chữ ký Refresh token không hợp lệ");
        }

        User user = refreshToken.getUser();
        CustomUserDetails userDetails = new CustomUserDetails(user);
        String newAccessToken = tokenProvider.generateAccessToken(userDetails);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(requestRefreshToken)
                .tokenType("Bearer")
                .expiresIn(tokenProvider.getAccessExpirationMs() / 1000)
                .user(userMapper.toResponse(user))
                .build();
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenRepository.findByToken(refreshToken).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    private void saveRefreshToken(User user, String tokenString) {
        // Xóa token cũ của user trước khi lưu mới (hoặc quản lý đa thiết bị)
        refreshTokenRepository.deleteByUser(user);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(tokenString)
                .expiresAt(Instant.now().plusMillis(tokenProvider.getRefreshExpirationMs()))
                .revoked(false)
                .build();

        refreshTokenRepository.save(refreshToken);
    }
}

