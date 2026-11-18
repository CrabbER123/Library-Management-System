package com.library.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "unit-test-secret-key-0123456789-abcdefgh";

    private static JwtTokenProvider provider(Duration accessTtl) {
        return new JwtTokenProvider(new JwtProperties(SECRET, accessTtl, Duration.ofDays(7)));
    }

    @Test
    void createsAndParsesAccessToken() {
        JwtTokenProvider provider = provider(Duration.ofMinutes(30));

        String token = provider.createAccessToken(7L, "admin", List.of("ADMIN", "LIBRARIAN"));
        Claims claims = provider.parse(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("7");
        assertThat(claims.get("username", String.class)).isEqualTo("admin");
        assertThat(claims.get("roles", List.class)).containsExactly("ADMIN", "LIBRARIAN");
        assertThat(provider.accessTokenTtlSeconds()).isEqualTo(1800L);
    }

    @Test
    void expiredTokenIsRejected() {
        JwtTokenProvider provider = provider(Duration.ofSeconds(-10));
        String token = provider.createAccessToken(1L, "reader01", List.of("READER"));

        assertThatThrownBy(() -> provider.parse(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtTokenProvider provider = provider(Duration.ofMinutes(30));
        String token = provider.createAccessToken(1L, "admin", List.of("ADMIN"));
        // 篡改 payload 的任意字符，签名校验必然失败
        String tampered = token.substring(0, token.length() - 2) + "aa";

        assertThatThrownBy(() -> provider.parse(tampered))
                .isInstanceOf(io.jsonwebtoken.JwtException.class);
    }

    @Test
    void rejectsSecretShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtTokenProvider(
                new JwtProperties("too-short-secret", Duration.ofMinutes(30), Duration.ofDays(7))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }
}
