package com.library.modules.auth.dto;

import java.util.List;

/**
 * 当前登录用户信息。
 *
 * @param readerId 关联的读者档案 ID，非读者账号为 null
 */
public record CurrentUserVO(
        Long id,
        String username,
        String displayName,
        List<String> roles,
        Long readerId
) {
}
