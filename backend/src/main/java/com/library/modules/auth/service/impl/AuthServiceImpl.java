package com.library.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.modules.auth.dto.CurrentUserVO;
import com.library.modules.auth.dto.LoginRequest;
import com.library.modules.auth.dto.TokenResponse;
import com.library.modules.auth.entity.RefreshToken;
import com.library.modules.auth.mapper.RefreshTokenMapper;
import com.library.modules.auth.service.AuthService;
import com.library.modules.reader.entity.Reader;
import com.library.modules.reader.mapper.ReaderMapper;
import com.library.modules.user.constant.UserStatus;
import com.library.modules.user.dto.UserVO;
import com.library.modules.user.entity.User;
import com.library.modules.user.mapper.UserMapper;
import com.library.modules.user.service.RoleAssignmentService;
import com.library.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserMapper userMapper;
    private final RefreshTokenMapper refreshTokenMapper;
    private final ReaderMapper readerMapper;
    private final RoleAssignmentService roleAssignmentService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery()
                .eq(User::getUsername, request.username().trim()));
        if (user == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }
        userMapper.update(null, Wrappers.<User>lambdaUpdate()
                .eq(User::getId, user.getId())
                .set(User::getLastLoginAt, LocalDateTime.now()));

        List<String> roles = roleAssignmentService.codesOf(user.getId());
        return issueTokens(user, roles);
    }

    @Override
    @Transactional
    public TokenResponse refresh(String refreshToken) {
        RefreshToken stored = refreshTokenMapper.selectOne(Wrappers.<RefreshToken>lambdaQuery()
                .eq(RefreshToken::getTokenHash, sha256(refreshToken)));
        if (stored == null) {
            throw new BizException(ErrorCode.INVALID_TOKEN);
        }
        if (Boolean.TRUE.equals(stored.getRevoked())) {
            throw new BizException(ErrorCode.INVALID_TOKEN, "刷新令牌已失效，请重新登录");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BizException(ErrorCode.TOKEN_EXPIRED);
        }
        User user = userMapper.selectById(stored.getUserId());
        if (user == null) {
            throw new BizException(ErrorCode.INVALID_TOKEN);
        }
        if (!UserStatus.ACTIVE.equals(user.getStatus())) {
            throw new BizException(ErrorCode.ACCOUNT_DISABLED);
        }

        // 轮换：旧的立即作废
        refreshTokenMapper.update(null, Wrappers.<RefreshToken>lambdaUpdate()
                .eq(RefreshToken::getId, stored.getId())
                .set(RefreshToken::getRevoked, true)
                .set(RefreshToken::getLastUsedAt, LocalDateTime.now()));

        return issueTokens(user, roleAssignmentService.codesOf(user.getId()));
    }

    @Override
    @Transactional
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            return;
        }
        refreshTokenMapper.update(null, Wrappers.<RefreshToken>lambdaUpdate()
                .eq(RefreshToken::getTokenHash, sha256(refreshToken))
                .set(RefreshToken::getRevoked, true));
    }

    @Override
    public CurrentUserVO me(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED);
        }
        Reader reader = readerMapper.selectOne(Wrappers.<Reader>lambdaQuery().eq(Reader::getUserId, userId));
        return new CurrentUserVO(user.getId(), user.getUsername(), user.getDisplayName(),
                roleAssignmentService.codesOf(userId), reader == null ? null : reader.getId());
    }

    private TokenResponse issueTokens(User user, List<String> roles) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), roles);
        String refreshToken = createRefreshToken(user.getId());
        UserVO userVO = new UserVO(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getPhone(), user.getStatus(), roles, user.getCreatedAt(), user.getLastLoginAt());
        return new TokenResponse(accessToken, refreshToken, "Bearer",
                jwtTokenProvider.accessTokenTtlSeconds(), userVO);
    }

    private String createRefreshToken(Long userId) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        RefreshToken token = new RefreshToken();
        token.setUserId(userId);
        token.setTokenHash(sha256(raw));
        token.setExpiresAt(LocalDateTime.now().plus(jwtTokenProvider.properties().refreshTokenTtl()));
        token.setRevoked(false);
        refreshTokenMapper.insert(token);
        return raw;
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("JVM 不支持 SHA-256", ex);
        }
    }
}
