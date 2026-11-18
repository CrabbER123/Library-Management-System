package com.library.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT 配置，对应 application.yml 中的 library.jwt.*。
 *
 * @param secret          HMAC 密钥，至少 32 字节；生产环境必须通过环境变量注入
 * @param accessTokenTtl  access token 有效期
 * @param refreshTokenTtl refresh token 有效期
 */
@ConfigurationProperties(prefix = "library.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {

    public JwtProperties {
        if (accessTokenTtl == null) {
            accessTokenTtl = Duration.ofMinutes(30);
        }
        if (refreshTokenTtl == null) {
            refreshTokenTtl = Duration.ofDays(7);
        }
    }
}
