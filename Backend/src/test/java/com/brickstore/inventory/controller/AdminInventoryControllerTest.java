package com.brickstore.inventory.controller;

import com.brickstore.catalog.entity.ProductVariant;
import com.brickstore.catalog.repository.ProductVariantRepository;
import com.brickstore.inventory.dto.StockAdjustmentRequest;
import com.brickstore.inventory.enums.InventoryActionType;
import com.brickstore.user.dto.request.LoginRequest;
import com.brickstore.user.dto.request.RegisterRequest;
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

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminInventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    private String adminToken;
    private String userToken;
    private Long variantId;

    @BeforeEach
    void setUp() throws Exception {
        ProductVariant variant = productVariantRepository.findAll().get(0);
        variantId = variant.getId();

        // Đăng nhập Admin
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@brickstore.com")
                .password("admin123")
                .build();

        MvcResult adminResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode adminJson = objectMapper.readTree(adminResult.getResponse().getContentAsString());
        adminToken = adminJson.path("data").path("accessToken").asText();

        // Đăng ký User thường
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("testuser_inv@brickstore.com")
                .password("user123")
                .fullName("Test User Inv")
                .phone("0982223344")
                .build();

        MvcResult userResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode userJson = objectMapper.readTree(userResult.getResponse().getContentAsString());
        userToken = userJson.path("data").path("accessToken").asText();
    }

    @Test
    @DisplayName("Admin xem danh sách tồn kho toàn bộ SKU có phân trang")
    void getAllInventory_Admin_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].sku", notNullValue()));
    }

    @Test
    @DisplayName("Admin điều chỉnh tồn kho (nhập kho) thành công")
    void adjustStock_Admin_Success() throws Exception {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .changeQuantity(8)
                .actionType(InventoryActionType.RESTOCK)
                .note("Nhập hàng từ kho tổng")
                .build();

        mockMvc.perform(put("/api/v1/admin/inventory/" + variantId + "/adjust")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.availableStock", greaterThanOrEqualTo(8)));
    }

    @Test
    @DisplayName("User thường cố điều chỉnh tồn kho -> 403 Forbidden")
    void adjustStock_User_Forbidden() throws Exception {
        StockAdjustmentRequest request = StockAdjustmentRequest.builder()
                .changeQuantity(5)
                .build();

        mockMvc.perform(put("/api/v1/admin/inventory/" + variantId + "/adjust")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Khách kiểm tra tồn kho công khai qua endpoint check")
    void checkStock_Public_Success() throws Exception {
        mockMvc.perform(get("/api/v1/inventory/" + variantId + "/check")
                        .param("quantity", "2")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.available", is(true)));
    }

    @Test
    @DisplayName("Admin xem lịch sử biến động kho của SKU")
    void getInventoryLogs_Admin_Success() throws Exception {
        mockMvc.perform(get("/api/v1/admin/inventory/" + variantId + "/logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()));
    }
}

