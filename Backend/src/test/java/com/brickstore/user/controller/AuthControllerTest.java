package com.brickstore.user.controller;

import com.brickstore.user.dto.request.LoginRequest;
import com.brickstore.user.dto.request.RefreshTokenRequest;
import com.brickstore.user.dto.request.RegisterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Đăng ký tài khoản mới thành công")
    void register_Success() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("testuser@brickstore.com")
                .password("password123")
                .fullName("Nguyễn Văn Test")
                .phone("0987654321")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("testuser@brickstore.com")))
                .andExpect(jsonPath("$.data.user.fullName", is("Nguyễn Văn Test")))
                .andExpect(jsonPath("$.data.user.roles[0]", is("ROLE_USER")));
    }

    @Test
    @DisplayName("Đăng ký với email trùng lặp -> 409 Conflict")
    void register_DuplicateEmail_Conflict() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("admin@brickstore.com") // Email admin đã có sẵn trong seed data
                .password("password123")
                .fullName("Trùng Lặp")
                .phone("0912345678")
                .build();

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("AUTH_004")));
    }

    @Test
    @DisplayName("Đăng nhập bằng tài khoản Admin mặc định thành công")
    void login_Admin_Success() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@brickstore.com")
                .password("admin123")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()))
                .andExpect(jsonPath("$.data.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.data.user.email", is("admin@brickstore.com")))
                .andExpect(jsonPath("$.data.user.roles[0]", is("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Đăng nhập sai mật khẩu -> 401 Unauthorized")
    void login_WrongPassword_Unauthorized() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("admin@brickstore.com")
                .password("wrongpassword")
                .build();

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("AUTH_003")));
    }

    @Test
    @DisplayName("Refresh Token hợp lệ thành công")
    void refreshToken_Success() throws Exception {
        // Bước 1: Login để lấy refreshToken
        LoginRequest loginReq = LoginRequest.builder()
                .email("admin@brickstore.com")
                .password("admin123")
                .build();

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(loginResult.getResponse().getContentAsString());
        String refreshToken = root.path("data").path("refreshToken").asText();

        // Bước 2: Gọi refresh token
        RefreshTokenRequest refreshReq = RefreshTokenRequest.builder()
                .refreshToken(refreshToken)
                .build();

        mockMvc.perform(post("/api/v1/auth/refresh-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.accessToken", notNullValue()));
    }
}

