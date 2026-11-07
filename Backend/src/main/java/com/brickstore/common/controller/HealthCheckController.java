package com.brickstore.common.controller;

import com.brickstore.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Health Check", description = "Kiểm tra trạng thái hoạt động của hệ thống")
public class HealthCheckController {

    @GetMapping
    @Operation(summary = "Kiểm tra kết nối hệ thống", description = "Trả về thông tin phiên bản và trạng thái hệ thống")
    public ResponseEntity<ApiResponse<Map<String, String>>> checkHealth() {
        Map<String, String> healthInfo = Map.of(
                "status", "UP",
                "service", "BrickStore Backend",
                "version", "1.0.0",
                "environment", "Development"
        );
        return ResponseEntity.ok(ApiResponse.success("Hệ thống BrickStore Backend đang hoạt động ổn định", healthInfo));
    }
}

