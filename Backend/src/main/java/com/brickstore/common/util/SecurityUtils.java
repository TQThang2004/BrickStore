package com.brickstore.common.util;

import com.brickstore.common.enums.ErrorCode;
import com.brickstore.common.exception.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn chưa đăng nhập");
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        } else if (principal instanceof String principalString) {
            if ("anonymousUser".equals(principalString)) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "Bạn chưa đăng nhập");
            }
            return principalString;
        }

        throw new BusinessException(ErrorCode.UNAUTHORIZED, "Không thể xác định thông tin người dùng");
    }
}

