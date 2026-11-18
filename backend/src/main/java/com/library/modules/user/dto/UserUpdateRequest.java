package com.library.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 更新账号资料请求。字段为 null 表示不修改；password 为空串表示不修改密码。
 */
public record UserUpdateRequest(

        @Size(max = 64, message = "姓名最多 64 个字符")
        String displayName,

        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱最多 128 个字符")
        String email,

        @Pattern(regexp = "^$|^[0-9+\\-() ]{5,32}$", message = "手机号格式不正确")
        String phone,

        @Size(max = 64, message = "密码最多 64 个字符")
        String password
) {
}
