package com.library.modules.auth.dto;

import com.library.modules.user.dto.UserVO;

/**
 * 登录/刷新成功响应。
 *
 * @param expiresIn access token 剩余有效秒数
 */
public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        UserVO user
) {
}
