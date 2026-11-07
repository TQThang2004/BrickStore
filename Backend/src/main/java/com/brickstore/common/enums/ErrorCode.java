package com.brickstore.common.enums;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // General & System
    INTERNAL_SERVER_ERROR("SYS_001", "Lỗi hệ thống nội bộ", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_REQUEST("SYS_002", "Yêu cầu không hợp lệ", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("SYS_003", "Không tìm thấy tài nguyên yêu cầu", HttpStatus.NOT_FOUND),
    METHOD_NOT_ALLOWED("SYS_004", "Phương thức HTTP không được hỗ trợ", HttpStatus.METHOD_NOT_ALLOWED),

    // Authentication & Authorization
    UNAUTHORIZED("AUTH_001", "Chưa xác thực hoặc token không hợp lệ", HttpStatus.UNAUTHORIZED),
    ACCESS_DENIED("AUTH_002", "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    INVALID_CREDENTIALS("AUTH_003", "Email hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    EMAIL_ALREADY_EXISTS("AUTH_004", "Email đã được sử dụng", HttpStatus.CONFLICT),
    TOKEN_EXPIRED("AUTH_005", "Phiên đăng nhập đã hết hạn", HttpStatus.UNAUTHORIZED),

    // Business & Inventory
    INSUFFICIENT_STOCK("INV_001", "Sản phẩm không đủ số lượng tồn kho", HttpStatus.BAD_REQUEST),
    PRODUCT_NOT_FOUND("CAT_001", "Không tìm thấy sản phẩm", HttpStatus.NOT_FOUND),
    VARIANT_NOT_FOUND("CAT_002", "Không tìm thấy biến thể sản phẩm", HttpStatus.NOT_FOUND),
    CATEGORY_NOT_FOUND("CAT_003", "Không tìm thấy danh mục", HttpStatus.NOT_FOUND),
    BRAND_NOT_FOUND("CAT_004", "Không tìm thấy thương hiệu", HttpStatus.NOT_FOUND),
    DUPLICATE_RESOURCE("SYS_005", "Dữ liệu đã tồn tại", HttpStatus.CONFLICT),
    CART_ITEM_NOT_FOUND("CART_001", "Không tìm thấy sản phẩm trong giỏ hàng", HttpStatus.NOT_FOUND),
    ORDER_NOT_FOUND("ORD_001", "Không tìm thấy đơn hàng", HttpStatus.NOT_FOUND),
    PAYMENT_FAILED("PAY_001", "Xử lý thanh toán thất bại", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }
}

