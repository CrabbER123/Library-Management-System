package com.library.modules.auth.controller;

import com.library.common.result.ApiResponse;
import com.library.modules.auth.dto.CurrentUserVO;
import com.library.modules.auth.dto.LoginRequest;
import com.library.modules.auth.dto.RefreshTokenRequest;
import com.library.modules.auth.dto.RegisterRequest;
import com.library.modules.auth.dto.TokenResponse;
import com.library.modules.auth.service.AuthService;
import com.library.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 登录、刷新令牌、退出与当前用户信息。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "认证")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "登录", description = "返回 accessToken（30 分钟）与 refreshToken（7 天）")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements
    @Operation(summary = "读者自助注册", description = "创建 READER 账号与借阅证，并直接返回令牌（免登录）")
    public ApiResponse<TokenResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.ok(authService.register(request));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "刷新令牌", description = "refreshToken 一次性使用，刷新后旧的立即失效")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.ok(authService.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "撤销当前 refreshToken")
    public ApiResponse<Void> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        authService.logout(request == null ? null : request.refreshToken());
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    @Operation(summary = "当前登录用户")
    public ApiResponse<CurrentUserVO> me() {
        return ApiResponse.ok(authService.me(SecurityUtils.currentUserId()));
    }
}
