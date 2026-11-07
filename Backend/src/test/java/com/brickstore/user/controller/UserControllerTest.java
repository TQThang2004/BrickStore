package com.brickstore.user.controller;

import com.brickstore.user.dto.request.ChangePasswordRequest;
import com.brickstore.user.dto.request.LoginRequest;
import com.brickstore.user.dto.request.UpdateProfileRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        LoginRequest loginReq = LoginRequest.builder()
                .email("admin@brickstore.com")
                .password("admin123")
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        adminToken = "Bearer " + root.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Truy cập /api/v1/users/me khi chưa đăng nhập -> 401 Unauthorized")
    void getMe_WithoutToken_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("AUTH_001")));
    }

    @Test
    @DisplayName("Truy cập /api/v1/users/me với JWT hợp lệ -> 200 OK")
    void getMe_WithToken_Success() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("admin@brickstore.com")))
                .andExpect(jsonPath("$.data.fullName", is("BrickStore Administrator")));
    }

    @Test
    @DisplayName("Cập nhật thông tin cá nhân thành công")
    void updateProfile_Success() throws Exception {
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Quản Trị Viên Mới")
                .phone("0911223344")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.fullName", is("Quản Trị Viên Mới")))
                .andExpect(jsonPath("$.data.phone", is("0911223344")));
    }

    @Test
    @DisplayName("Đổi mật khẩu với mật khẩu cũ sai -> 400 Bad Request")
    void changePassword_WrongCurrentPassword_BadRequest() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("saimatkhaurui")
                .newPassword("newPass123")
                .confirmPassword("newPass123")
                .build();

        mockMvc.perform(put("/api/v1/users/me/change-password")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)));
    }

    @Test
    @DisplayName("Đổi mật khẩu thành công")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = ChangePasswordRequest.builder()
                .currentPassword("admin123")
                .newPassword("adminNewPass456")
                .confirmPassword("adminNewPass456")
                .build();

        mockMvc.perform(put("/api/v1/users/me/change-password")
                        .header("Authorization", adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)));
    }
}

