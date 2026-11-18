package com.library.modules.author.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 作者新增/编辑请求（PUT 为全量覆盖语义）。
 */
public record AuthorRequest(
        @NotBlank(message = "作者姓名不能为空")
        @Size(max = 100, message = "作者姓名不能超过 100 字符")
        String name,

        @Size(max = 64, message = "国籍不能超过 64 字符")
        String nationality,

        @Size(max = 5000, message = "作者简介不能超过 5000 字符")
        String intro) {
}
