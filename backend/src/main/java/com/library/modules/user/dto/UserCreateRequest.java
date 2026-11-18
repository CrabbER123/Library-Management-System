package com.library.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 创建账号请求。
 */
public record UserCreateRequest(

        @NotBlank(message = "用户名不能为空")
        @Pattern(regexp = "^[a-zA-Z0-9_]{3,32}$", message = "用户名只能包含字母、数字、下划线，长度 3-32")
        String username,

        @NotBlank(message = "密码不能为空")
        @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
        String password,

        @NotBlank(message = "姓名不能为空")
        @Size(max = 64, message = "姓名最多 64 个字符")
        String displayName,

        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱最多 128 个字符")
        String email,

        @Pattern(regexp = "^$|^[0-9+\\-() ]{5,32}$", message = "手机号格式不正确")
        String phone,

        @NotEmpty(message = "至少选择一个角色")
        List<@NotBlank(message = "角色编码不能为空") String> roles
) {
}
