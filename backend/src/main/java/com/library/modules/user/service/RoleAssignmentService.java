package com.library.modules.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.modules.user.entity.Role;
import com.library.modules.user.entity.UserRole;
import com.library.modules.user.mapper.RoleMapper;
import com.library.modules.user.mapper.UserRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 用户-角色关联的统一读写入口，避免各模块重复实现批量装载逻辑。
 */
@Service
@RequiredArgsConstructor
public class RoleAssignmentService {

    private final UserRoleMapper userRoleMapper;
    private final RoleMapper roleMapper;

    public List<Role> allRoles() {
        return roleMapper.selectList(Wrappers.<Role>lambdaQuery().orderByAsc(Role::getId));
    }

    public List<String> codesOf(Long userId) {
        return codesOf(List.of(userId)).getOrDefault(userId, List.of());
    }

    /**
     * 批量查询角色，供分页列表使用，避免 N+1。
     */
    public Map<Long, List<String>> codesOf(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        List<UserRole> links = userRoleMapper.selectList(
                Wrappers.<UserRole>lambdaQuery().in(UserRole::getUserId, userIds));
        if (links.isEmpty()) {
            return Map.of();
        }
        List<Long> roleIds = links.stream().map(UserRole::getRoleId).distinct().toList();
        Map<Long, String> codeById = roleMapper.selectBatchIds(roleIds).stream()
                .collect(Collectors.toMap(Role::getId, Role::getCode));

        Map<Long, List<String>> result = new HashMap<>();
        for (UserRole link : links) {
            String code = codeById.get(link.getRoleId());
            if (code != null) {
                result.computeIfAbsent(link.getUserId(), k -> new ArrayList<>()).add(code);
            }
        }
        result.values().forEach(codes -> codes.sort(Comparator.naturalOrder()));
        return result;
    }

    @Transactional
    public void replace(Long userId, Collection<String> roleCodes) {
        userRoleMapper.delete(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getUserId, userId));
        if (roleCodes == null || roleCodes.isEmpty()) {
            return;
        }
        Set<String> distinctCodes = new HashSet<>(roleCodes);
        List<Role> roles = roleMapper.selectList(Wrappers.<Role>lambdaQuery().in(Role::getCode, distinctCodes));
        if (roles.size() != distinctCodes.size()) {
            Set<String> missing = new HashSet<>(distinctCodes);
            roles.forEach(role -> missing.remove(role.getCode()));
            throw new BizException(ErrorCode.VALIDATION_ERROR, "角色不存在: " + String.join(", ", missing));
        }
        for (Role role : roles) {
            UserRole link = new UserRole();
            link.setUserId(userId);
            link.setRoleId(role.getId());
            userRoleMapper.insert(link);
        }
    }

    public long countByRoleCode(String roleCode) {
        Role role = roleMapper.selectOne(Wrappers.<Role>lambdaQuery().eq(Role::getCode, roleCode));
        if (role == null) {
            return 0L;
        }
        Long count = userRoleMapper.selectCount(Wrappers.<UserRole>lambdaQuery().eq(UserRole::getRoleId, role.getId()));
        return count == null ? 0L : count;
    }
}
