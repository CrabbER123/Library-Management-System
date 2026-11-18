package com.library.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 更新账号角色（全量替换）。
 */
public record UserRolesRequest(

        @NotEmpty(message = "至少选择一个角色")
        List<@NotBlank(message = "角色编码不能为空") String> roles
) {
}
