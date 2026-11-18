package com.library.modules.borrow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 借书请求：由馆员在柜台为读者办理。
 */
public record BorrowRequest(
        @NotNull(message = "读者不能为空")
        Long readerId,

        @NotNull(message = "图书不能为空")
        Long bookId,

        @Size(max = 255, message = "备注不能超过 255 字符")
        String remark) {
}
