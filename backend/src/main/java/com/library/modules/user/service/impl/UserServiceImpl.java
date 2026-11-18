package com.library.modules.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.constant.RoleCodes;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.query.Sorts;
import com.library.common.result.PageResult;
import com.library.modules.auth.entity.RefreshToken;
import com.library.modules.auth.mapper.RefreshTokenMapper;
import com.library.modules.user.constant.UserStatus;
import com.library.modules.user.dto.RoleVO;
import com.library.modules.user.dto.UserCreateRequest;
import com.library.modules.user.dto.UserUpdateRequest;
import com.library.modules.user.dto.UserVO;
import com.library.modules.user.entity.User;
import com.library.modules.user.mapper.UserMapper;
import com.library.modules.user.service.RoleAssignmentService;
import com.library.modules.user.service.UserService;
import com.library.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "username", "username",
            "createdAt", "created_at",
            "lastLoginAt", "last_login_at");

    private final UserMapper userMapper;
    private final RoleAssignmentService roleAssignmentService;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    public PageResult<UserVO> list(int page, int size, String keyword, String role, String status, String sort) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        QueryWrapper<User> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("username", kw)
                    .or().like("display_name", kw)
                    .or().like("phone", kw));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        if (StringUtils.hasText(role)) {
            wrapper.apply("id IN (SELECT ur.user_id FROM user_roles ur "
                    + "JOIN roles r ON r.id = ur.role_id WHERE r.code = {0})", role.trim());
        }
        if (Sorts.apply(wrapper, sort, SORT_COLUMNS) == null) {
            Sorts.applyDefault(wrapper, "id", false);
        }

        Page<User> result = userMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        Map<Long, List<String>> roleMap = roleAssignmentService.codesOf(
                result.getRecords().stream().map(User::getId).toList());
        return PageResult.of(result, user -> toVO(user, roleMap.getOrDefault(user.getId(), List.of())));
    }

    @Override
    public UserVO getById(Long id) {
        User user = requireUser(id);
        return toVO(user, roleAssignmentService.codesOf(id));
    }

    @Override
    @Transactional
    public UserVO create(UserCreateRequest request) {
        String username = request.username().trim();
        Long exists = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getUsername, username));
        if (exists != null && exists > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "用户名已存在: " + username);
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user.setEmail(trimToNull(request.email()));
        user.setPhone(trimToNull(request.phone()));
        user.setStatus(UserStatus.ACTIVE);
        userMapper.insert(user);

        roleAssignmentService.replace(user.getId(), request.roles());
        return toVO(user, roleAssignmentService.codesOf(user.getId()));
    }

    @Override
    @Transactional
    public UserVO update(Long id, UserUpdateRequest request) {
        User user = requireUser(id);
        if (StringUtils.hasText(request.displayName())) {
            user.setDisplayName(request.displayName().trim());
        }
        if (request.email() != null) {
            user.setEmail(trimToNull(request.email()));
        }
        if (request.phone() != null) {
            user.setPhone(trimToNull(request.phone()));
        }
        if (StringUtils.hasText(request.password())) {
            String password = request.password();
            if (password.length() < 6) {
                throw new BizException(ErrorCode.BAD_REQUEST, "密码长度至少 6 位");
            }
            user.setPasswordHash(passwordEncoder.encode(password));
            // 修改密码后立即失效所有刷新令牌
            refreshTokenMapper.delete(Wrappers.<RefreshToken>lambdaQuery().eq(RefreshToken::getUserId, id));
        }
        userMapper.updateById(user);
        return toVO(user, roleAssignmentService.codesOf(id));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        User user = requireUser(id);
        if (!UserStatus.ALL.contains(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不支持的状态: " + status);
        }
        if (UserStatus.DISABLED.equals(status)) {
            if (id.equals(SecurityUtils.currentUserId())) {
                throw new BizException(ErrorCode.CANNOT_OPERATE_SELF);
            }
            ensureNotLastAdmin(id);
            refreshTokenMapper.delete(Wrappers.<RefreshToken>lambdaQuery().eq(RefreshToken::getUserId, id));
        }
        user.setStatus(status);
        userMapper.updateById(user);
    }

    @Override
    @Transactional
    public void updateRoles(Long id, List<String> roles) {
        requireUser(id);
        boolean removingAdmin = roleAssignmentService.codesOf(id).contains(RoleCodes.ADMIN)
                && !roles.contains(RoleCodes.ADMIN);
        if (removingAdmin) {
            ensureNotLastAdmin(id);
        }
        roleAssignmentService.replace(id, roles);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireUser(id);
        if (id.equals(SecurityUtils.currentUserId())) {
            throw new BizException(ErrorCode.CANNOT_OPERATE_SELF);
        }
        ensureNotLastAdmin(id);
        userMapper.deleteById(id);
    }

    @Override
    public List<RoleVO> listRoles() {
        return roleAssignmentService.allRoles().stream()
                .map(role -> new RoleVO(role.getId(), role.getCode(), role.getName(), role.getDescription()))
                .toList();
    }

    private void ensureNotLastAdmin(Long userId) {
        boolean isAdmin = roleAssignmentService.codesOf(userId).contains(RoleCodes.ADMIN);
        if (isAdmin && roleAssignmentService.countByRoleCode(RoleCodes.ADMIN) <= 1) {
            throw new BizException(ErrorCode.LAST_ADMIN);
        }
    }

    private User requireUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "用户不存在: " + id);
        }
        return user;
    }

    private UserVO toVO(User user, List<String> roles) {
        return new UserVO(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail(),
                user.getPhone(), user.getStatus(), roles, user.getCreatedAt(), user.getLastLoginAt());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
