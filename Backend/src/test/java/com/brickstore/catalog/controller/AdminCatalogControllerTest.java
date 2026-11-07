package com.brickstore.catalog.controller;

import com.brickstore.catalog.dto.*;
import com.brickstore.catalog.repository.CategoryRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCatalogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    private String adminToken;
    private String userToken;

    @BeforeEach
    void setUp() throws Exception {
        // Đăng nhập Admin lấy token
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

        // Đăng ký User thường lấy token
        RegisterRequest registerReq = RegisterRequest.builder()
                .email("testuser_catalog@brickstore.com")
                .password("user123")
                .fullName("Test User Catalog")
                .phone("0981112233")
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
    @DisplayName("Admin tạo Category mới thành công")
    void createCategory_Admin_Success() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("LEGO Ninjago Series")
                .description("Dòng sản phẩm LEGO Ninjago")
                .sortOrder(5)
                .build();

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("LEGO Ninjago Series")))
                .andExpect(jsonPath("$.data.slug", is("lego-ninjago-series")));
    }

    @Test
    @DisplayName("User thường gọi API Admin tạo Category -> 403 Forbidden")
    void createCategory_User_Forbidden() throws Exception {
        CreateCategoryRequest request = CreateCategoryRequest.builder()
                .name("Unauthorized Category")
                .build();

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin tạo Brand mới thành công")
    void createBrand_Admin_Success() throws Exception {
        CreateBrandRequest request = CreateBrandRequest.builder()
                .name("Bandai Namco")
                .description("Hãng mô hình Gunpla Nhật Bản")
                .build();

        mockMvc.perform(post("/api/v1/admin/brands")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("Bandai Namco")))
                .andExpect(jsonPath("$.data.slug", is("bandai-namco")));
    }

    @Test
    @DisplayName("Admin tạo Sản phẩm kèm Biến thể SKU và Hình ảnh thành công")
    void createProduct_WithVariantsAndImages_Success() throws Exception {
        Long catId = categoryRepository.findAll().get(0).getId();

        CreateProductVariantRequest variant1 = CreateProductVariantRequest.builder()
                .sku("LEGO-CUSTOM-001")
                .name("Bản tiêu chuẩn")
                .price(BigDecimal.valueOf(1500000))
                .stockQuantity(10)
                .attributes(Map.of("color", "red", "pieces", 450))
                .build();

        ProductImageRequest img1 = ProductImageRequest.builder()
                .imageUrl("https://images.brickstore.com/custom1.png")
                .isPrimary(true)
                .sortOrder(0)
                .build();

        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(catId)
                .name("LEGO Creator Expert Xe Cổ Điển")
                .description("Mô hình xe hơi cổ điển chi tiết")
                .basePrice(BigDecimal.valueOf(1500000))
                .thumbnailUrl("https://images.brickstore.com/thumb.png")
                .variants(List.of(variant1))
                .images(List.of(img1))
                .build();

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", is("LEGO Creator Expert Xe Cổ Điển")))
                .andExpect(jsonPath("$.data.slug", is("lego-creator-expert-xe-co-dien")))
                .andExpect(jsonPath("$.data.variants", hasSize(1)))
                .andExpect(jsonPath("$.data.variants[0].sku", is("LEGO-CUSTOM-001")))
                .andExpect(jsonPath("$.data.images", hasSize(1)));
    }

    @Test
    @DisplayName("Admin tạo Sản phẩm với SKU đã tồn tại -> 409 Conflict")
    void createProduct_DuplicateSku_Conflict() throws Exception {
        Long catId = categoryRepository.findAll().get(0).getId();

        // SKU LEGO-75192-STD đã được seed trong Flyway V2
        CreateProductVariantRequest duplicateVariant = CreateProductVariantRequest.builder()
                .sku("LEGO-75192-STD")
                .name("Bản trùng SKU")
                .price(BigDecimal.valueOf(2000000))
                .stockQuantity(5)
                .build();

        CreateProductRequest request = CreateProductRequest.builder()
                .categoryId(catId)
                .name("LEGO Sản phẩm trùng SKU")
                .basePrice(BigDecimal.valueOf(2000000))
                .variants(List.of(duplicateVariant))
                .build();

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SYS_005")));
    }
}
