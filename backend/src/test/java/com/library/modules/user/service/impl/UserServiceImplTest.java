package com.library.modules.user.service.impl;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.modules.auth.entity.RefreshToken;
import com.library.modules.auth.mapper.RefreshTokenMapper;
import com.library.modules.user.constant.UserStatus;
import com.library.modules.user.dto.UserCreateRequest;
import com.library.modules.user.dto.UserUpdateRequest;
import com.library.modules.user.dto.UserVO;
import com.library.modules.user.entity.User;
import com.library.modules.user.mapper.UserMapper;
import com.library.modules.user.service.RoleAssignmentService;
import com.library.security.AuthUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private static final Long CURRENT_USER_ID = 1L;

    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleAssignmentService roleAssignmentService;
    @Mock
    private RefreshTokenMapper refreshTokenMapper;

    private PasswordEncoder passwordEncoder;
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4);
        userService = new UserServiceImpl(userMapper, roleAssignmentService, passwordEncoder, refreshTokenMapper);

        AuthUser principal = new AuthUser(CURRENT_USER_ID, "admin", List.of("ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createRejectsDuplicateUsername() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> userService.create(
                new UserCreateRequest("admin", "secret123", "管理员", null, null, List.of("ADMIN"))))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE));
    }

    @Test
    void createHashesPasswordAndAssignsRoles() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(9L);
            return 1;
        });
        when(roleAssignmentService.codesOf(9L)).thenReturn(List.of("LIBRARIAN"));

        UserVO created = userService.create(
                new UserCreateRequest("librarian2", "secret123", "李馆员", null, null, List.of("LIBRARIAN")));

        assertThat(created.id()).isEqualTo(9L);
        assertThat(created.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.roles()).containsExactly("LIBRARIAN");
        // 落库的是 BCrypt 哈希，而不是明文
        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", captor.getValue().getPasswordHash())).isTrue();
    }

    @Test
    void updatePasswordRevokesRefreshTokens() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, "librarian"));
        when(roleAssignmentService.codesOf(2L)).thenReturn(List.of("LIBRARIAN"));

        userService.update(2L, new UserUpdateRequest(null, null, null, "new-secret"));

        verify(refreshTokenMapper).delete(any());
        verify(userMapper).updateById(any(User.class));
    }

    @Test
    void updateRejectsTooShortPassword() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, "librarian"));

        assertThatThrownBy(() -> userService.update(2L, new UserUpdateRequest(null, null, null, "123")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BAD_REQUEST));
    }

    @Test
    void disableSelfIsRejected() {
        when(userMapper.selectById(CURRENT_USER_ID)).thenReturn(user(CURRENT_USER_ID, "admin"));

        assertThatThrownBy(() -> userService.updateStatus(CURRENT_USER_ID, UserStatus.DISABLED))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_OPERATE_SELF));
    }

    @Test
    void deleteSelfIsRejected() {
        when(userMapper.selectById(CURRENT_USER_ID)).thenReturn(user(CURRENT_USER_ID, "admin"));

        assertThatThrownBy(() -> userService.delete(CURRENT_USER_ID))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CANNOT_OPERATE_SELF));
    }

    @Test
    void deleteLastAdminIsRejected() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, "admin2"));
        when(roleAssignmentService.codesOf(2L)).thenReturn(List.of("ADMIN"));
        when(roleAssignmentService.countByRoleCode("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.delete(2L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LAST_ADMIN));
    }

    @Test
    void removingLastAdminRoleIsRejected() {
        when(userMapper.selectById(2L)).thenReturn(user(2L, "admin2"));
        when(roleAssignmentService.codesOf(2L)).thenReturn(List.of("ADMIN"));
        when(roleAssignmentService.countByRoleCode("ADMIN")).thenReturn(1L);

        assertThatThrownBy(() -> userService.updateRoles(2L, List.of("LIBRARIAN")))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.LAST_ADMIN));
    }

    @Test
    void deleteNormalUserSucceeds() {
        when(userMapper.selectById(3L)).thenReturn(user(3L, "librarian"));
        when(roleAssignmentService.codesOf(3L)).thenReturn(List.of("LIBRARIAN"));

        userService.delete(3L);

        verify(userMapper).deleteById(eq(3L));
    }

    private static User user(Long id, String username) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setDisplayName(username);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash("$2a$10$abcdefghijklmnopqrstuv");
        return user;
    }
}
