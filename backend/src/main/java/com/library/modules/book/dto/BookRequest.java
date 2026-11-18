package com.library.modules.book.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 图书新增/编辑请求（PUT 为全量覆盖语义）。ISBN 允许带连字符，服务端会规范化后校验。
 */
public record BookRequest(
        @NotBlank(message = "ISBN 不能为空")
        @Size(max = 20, message = "ISBN 不能超过 20 字符")
        String isbn,

        @NotBlank(message = "书名不能为空")
        @Size(max = 200, message = "书名不能超过 200 字符")
        String title,

        @Size(max = 200, message = "副标题不能超过 200 字符")
        String subtitle,

        /**
         * 作者 ID（老接口兼容）；新前端直接提交 authorNames，服务端按名称复用或自动建档。
         */
        @Size(max = 20, message = "作者数量不能超过 20 位")
        List<Long> authorIds,

        /**
         * 作者姓名（自由文本，可多值）。与 authorIds 至少提供一种。
         */
        @Size(max = 20, message = "作者数量不能超过 20 位")
        List<@NotBlank(message = "作者姓名不能为空") @Size(max = 100, message = "作者姓名不能超过 100 字符") String> authorNames,

        Long publisherId,

        /**
         * 出版社名称（自由文本）。填写后若不存在会自动创建出版社。
         */
        @Size(max = 100, message = "出版社名称不能超过 100 字符")
        String publisherName,

        Long categoryId,

        LocalDate publishDate,

        @Size(max = 32, message = "语言不能超过 32 字符")
        String language,

        @DecimalMin(value = "0.0", message = "价格不能为负数")
        BigDecimal price,

        @Size(max = 512, message = "封面地址不能超过 512 字符")
        String coverUrl,

        @Size(max = 20000, message = "内容简介不能超过 20000 字符")
        String summary,

        @NotNull(message = "总库存不能为空")
        @Min(value = 0, message = "总库存不能小于 0")
        @Max(value = 100000, message = "总库存不能超过 100000")
        Integer totalStock,

        String status) {
}
