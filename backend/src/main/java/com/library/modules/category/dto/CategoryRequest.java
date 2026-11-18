package com.library.modules.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 分类新增/编辑请求（PUT 为全量覆盖语义）。
 */
public record CategoryRequest(
        @NotBlank(message = "分类名称不能为空")
        @Size(max = 64, message = "分类名称不能超过 64 字符")
        String name,

        @Size(max = 32, message = "分类编码不能超过 32 字符")
        String code,

        @Size(max = 255, message = "分类描述不能超过 255 字符")
        String description) {
}
