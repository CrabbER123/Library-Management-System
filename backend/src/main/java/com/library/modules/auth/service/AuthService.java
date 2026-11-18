package com.library.modules.auth.service;

import com.library.modules.auth.dto.CurrentUserVO;
import com.library.modules.auth.dto.LoginRequest;
import com.library.modules.auth.dto.RegisterRequest;
import com.library.modules.auth.dto.TokenResponse;

public interface AuthService {

    TokenResponse login(LoginRequest request);

    /**
     * 读者自助注册：创建 READER 账号与读者档案，并直接返回登录令牌。
     */
    TokenResponse register(RegisterRequest request);

    /**
     * 使用 refresh token 换取新的令牌对（旧 refresh token 立即失效）。
     */
    TokenResponse refresh(String refreshToken);

    void logout(String refreshToken);

    CurrentUserVO me(Long userId);
}
