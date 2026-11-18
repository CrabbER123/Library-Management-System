package com.library.security;

import java.util.List;

/**
 * 放入 SecurityContext 的当前登录用户。
 *
 * @param id       用户主键
 * @param username 登录名
 * @param roles    角色编码，如 ADMIN / LIBRARIAN / READER
 */
public record AuthUser(Long id, String username, List<String> roles) {

    public List<String> roleCodes() {
        return roles == null ? List.of() : roles;
    }

    public boolean hasRole(String role) {
        return roleCodes().contains(role);
    }
}
