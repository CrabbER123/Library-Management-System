package com.library.modules.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 刷新令牌 / 退出登录请求。
 */
public record RefreshTokenRequest(

        @NotBlank(message = "refreshToken 不能为空")
        String refreshToken
) {
}
