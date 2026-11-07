package com.brickstore.common.constant;

public final class SecurityConstant {

    private SecurityConstant() {
        // Prevent instantiation
    }

    public static final String TOKEN_PREFIX = "Bearer ";
    public static final String HEADER_STRING = "Authorization";

    public static final String ROLE_USER = "ROLE_USER";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";

    public static final String[] PUBLIC_URLS = {
        "/api/v1/auth/**",
        "/api/v1/products/**",
        "/api/v1/categories/**",
        "/api/v1/brands/**",
        "/api/v1/inventory/**",
        "/api/v1/search/**",
        "/api/v1/webhooks/**",
        "/v3/api-docs/**",
        "/swagger-ui/**",
        "/swagger-ui.html",
        "/actuator/health"
    };
}

