package com.library.modules.auth.service.impl;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.modules.auth.dto.CurrentUserVO;
import com.library.modules.auth.dto.LoginRequest;
import com.library.modules.auth.dto.TokenResponse;
import com.library.modules.auth.entity.RefreshToken;
import com.library.modules.auth.mapper.RefreshTokenMapper;
import com.library.modules.reader.entity.Reader;
import com.library.modules.reader.mapper.ReaderMapper;
import com.library.modules.user.constant.UserStatus;
import com.library.modules.user.entity.User;
import com.library.modules.user.mapper.UserMapper;
import com.library.modules.user.service.RoleAssignmentService;
import com.library.security.JwtProperties;
import com.library.security.JwtTokenProvider;
import com.library.support.MpTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    private static final String SECRET = "unit-test-secret-key-0123456789-abcdefgh";

    @Mock
    private UserMapper userMapper;
    @Mock
    private RefreshTokenMapper refreshTokenMapper;
    @Mock
    private ReaderMapper readerMapper;
    @Mock
    private RoleAssignmentService roleAssignmentService;

    private PasswordEncoder passwordEncoder;
    private JwtTokenProvider jwtTokenProvider;
    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        MpTestSupport.initTableInfo(User.class, RefreshToken.class, Reader.class);
        passwordEncoder = new BCryptPasswordEncoder(4);
        jwtTokenProvider = new JwtTokenProvider(new JwtProperties(SECRET, Duration.ofMinutes(30), Duration.ofDays(7)));
        authService = new AuthServiceImpl(userMapper, refreshTokenMapper, readerMapper,
                roleAssignmentService, passwordEncoder, jwtTokenProvider);
    }

    @Test
    void loginReturnsTokensAndUpdatesLastLogin() {
        User user = user(1L, "admin", "Admin@123456", UserStatus.ACTIVE);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(roleAssignmentService.codesOf(1L)).thenReturn(List.of("ADMIN"));
        when(refreshTokenMapper.insert(any(RefreshToken.class))).thenReturn(1);

        TokenResponse response = authService.login(new LoginRequest("admin", "Admin@123456"));

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(1800L);
        assertThat(response.user().username()).isEqualTo("admin");
        assertThat(response.user().roles()).containsExactly("ADMIN");
        // access token 可被解析回同一用户
        assertThat(jwtTokenProvider.parse(response.accessToken()).getPayload().getSubject()).isEqualTo("1");
        // 登录时间被刷新
        verify(userMapper).update(isNull(), any());
    }

    @Test
    void loginRejectsWrongPassword() {
        when(userMapper.selectOne(any())).thenReturn(user(1L, "admin", "Admin@123456", UserStatus.ACTIVE));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "wrong-password")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void loginRejectsUnknownUser() {
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ghost", "whatever")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    void loginRejectsDisabledAccount() {
        when(userMapper.selectOne(any())).thenReturn(user(1L, "admin", "Admin@123456", UserStatus.DISABLED));

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin", "Admin@123456")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.ACCOUNT_DISABLED));
    }

    @Test
    void refreshRotatesToken() {
        RefreshToken stored = refreshToken(9L, 1L, false, LocalDateTime.now().plusDays(1));
        when(refreshTokenMapper.selectOne(any())).thenReturn(stored);
        when(userMapper.selectById(1L)).thenReturn(user(1L, "admin", "Admin@123456", UserStatus.ACTIVE));
        when(roleAssignmentService.codesOf(1L)).thenReturn(List.of("ADMIN"));
        when(refreshTokenMapper.insert(any(RefreshToken.class))).thenReturn(1);

        TokenResponse response = authService.refresh("raw-refresh-token");

        assertThat(response.accessToken()).isNotBlank();
        assertThat(response.refreshToken()).isNotBlank();
        // 旧令牌被置为 revoked
        verify(refreshTokenMapper).update(isNull(), any());
    }

    @Test
    void refreshRejectsRevokedToken() {
        when(refreshTokenMapper.selectOne(any()))
                .thenReturn(refreshToken(9L, 1L, true, LocalDateTime.now().plusDays(1)));

        assertThatThrownBy(() -> authService.refresh("raw-refresh-token"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.INVALID_TOKEN));
    }

    @Test
    void refreshRejectsExpiredToken() {
        when(refreshTokenMapper.selectOne(any()))
                .thenReturn(refreshToken(9L, 1L, false, LocalDateTime.now().minusMinutes(1)));

        assertThatThrownBy(() -> authService.refresh("raw-refresh-token"))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.TOKEN_EXPIRED));
    }

    @Test
    void meIncludesReaderId() {
        when(userMapper.selectById(3L)).thenReturn(user(3L, "reader01", "Reader@123456", UserStatus.ACTIVE));
        when(roleAssignmentService.codesOf(3L)).thenReturn(List.of("READER"));
        Reader reader = new Reader();
        reader.setId(1L);
        reader.setUserId(3L);
        when(readerMapper.selectOne(any())).thenReturn(reader);

        CurrentUserVO me = authService.me(3L);

        assertThat(me.username()).isEqualTo("reader01");
        assertThat(me.roles()).containsExactly("READER");
        assertThat(me.readerId()).isEqualTo(1L);
    }

    @Test
    void logoutRevokesTokenSilently() {
        authService.logout("raw-refresh-token");

        verify(refreshTokenMapper).update(isNull(), any());
    }

    private static User user(Long id, String username, String rawPassword, String status) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setStatus(status);
        user.setPasswordHash(new BCryptPasswordEncoder(4).encode(rawPassword));
        return user;
    }

    private static RefreshToken refreshToken(Long id, Long userId, boolean revoked, LocalDateTime expiresAt) {
        RefreshToken token = new RefreshToken();
        token.setId(id);
        token.setUserId(userId);
        token.setTokenHash("hash");
        token.setRevoked(revoked);
        token.setExpiresAt(expiresAt);
        return token;
    }
}
