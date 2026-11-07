package com.brickstore.catalog.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Lấy danh sách sản phẩm công khai - Phân trang thành công")
    void getProducts_Success() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", notNullValue()))
                .andExpect(jsonPath("$.data.totalElements", greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("Tìm kiếm sản phẩm theo từ khóa 'Millennium'")
    void getProducts_FilterKeyword_Success() throws Exception {
        mockMvc.perform(get("/api/v1/products")
                        .param("keyword", "Millennium")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.content", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.content[0].name", containsString("Millennium Falcon")));
    }

    @Test
    @DisplayName("Lấy chi tiết sản phẩm theo slug hợp lệ")
    void getProductBySlug_Success() throws Exception {
        mockMvc.perform(get("/api/v1/products/lego-star-wars-75192-millennium-falcon-ucs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.name", containsString("Millennium Falcon")))
                .andExpect(jsonPath("$.data.variants", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data.images", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("Lấy chi tiết sản phẩm với slug không tồn tại -> 404 Not Found")
    void getProductBySlug_NotFound() throws Exception {
        mockMvc.perform(get("/api/v1/products/non-existent-product-slug")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.errorCode", is("SYS_003")));
    }

    @Test
    @DisplayName("Lấy danh sách sản phẩm nổi bật")
    void getFeaturedProducts_Success() throws Exception {
        mockMvc.perform(get("/api/v1/products/featured")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", notNullValue()));
    }

    @Test
    @DisplayName("Lấy danh sách cây danh mục")
    void getCategoryTree_Success() throws Exception {
        mockMvc.perform(get("/api/v1/categories/tree")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.data[0].subCategories", notNullValue()));
    }

    @Test
    @DisplayName("Lấy danh sách thương hiệu công khai")
    void getAllBrands_Success() throws Exception {
        mockMvc.perform(get("/api/v1/brands")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }
}

