package com.library.modules.book.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 调整总库存：可借库存按“总库存 - 已借出数量”自动重算。
 */
public record BookStockRequest(
        @NotNull(message = "总库存不能为空")
        @Min(value = 0, message = "总库存不能小于 0")
        @Max(value = 100000, message = "总库存不能超过 100000")
        Integer totalStock) {
}
