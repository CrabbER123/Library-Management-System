package com.library.modules.reader.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 读者新增/编辑请求（PUT 为全量覆盖语义）。
 * status、maxBorrow 为空时分别取 ACTIVE 与全局默认值；userId 用于绑定已有的读者登录账号。
 */
public record ReaderRequest(
        @NotBlank(message = "借阅证号不能为空")
        @Size(max = 32, message = "借阅证号不能超过 32 字符")
        String cardNo,

        @NotBlank(message = "读者姓名不能为空")
        @Size(max = 64, message = "读者姓名不能超过 64 字符")
        String name,

        @Size(max = 32, message = "联系电话不能超过 32 字符")
        String phone,

        @Email(message = "邮箱格式不正确")
        @Size(max = 128, message = "邮箱不能超过 128 字符")
        String email,

        @Pattern(regexp = "ACTIVE|SUSPENDED", message = "状态只能是 ACTIVE 或 SUSPENDED")
        String status,

        @Min(value = 0, message = "最大借阅数不能小于 0")
        @Max(value = 100, message = "最大借阅数不能超过 100")
        Integer maxBorrow,

        Long userId) {
}
