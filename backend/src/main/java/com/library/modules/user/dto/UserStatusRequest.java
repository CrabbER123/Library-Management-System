package com.library.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * 启用/禁用账号。
 */
public record UserStatusRequest(

        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "ACTIVE|DISABLED", message = "状态只能是 ACTIVE 或 DISABLED")
        String status
) {
}
