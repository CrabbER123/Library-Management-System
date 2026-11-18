package com.library.modules.publisher.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 出版社新增/编辑请求（PUT 为全量覆盖语义）。
 */
public record PublisherRequest(
        @NotBlank(message = "出版社名称不能为空")
        @Size(max = 100, message = "出版社名称不能超过 100 字符")
        String name,

        @Size(max = 255, message = "地址不能超过 255 字符")
        String address,

        @Size(max = 255, message = "网址不能超过 255 字符")
        String website,

        @Size(max = 32, message = "电话不能超过 32 字符")
        String phone) {
}
