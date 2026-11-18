package com.library.modules.user.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 用户视图，不包含密码。
 */
public record UserVO(
        Long id,
        String username,
        String displayName,
        String email,
        String phone,
        String status,
        List<String> roles,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt
) {
}
