package com.library.modules.borrow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 借书请求：馆员在柜台为读者办理时指定 {@code readerId}；
 * 读者自助借书时可省略，服务端会强制使用当前账号绑定的读者档案。
 */
public record BorrowRequest(
        Long readerId,

        @NotNull(message = "图书不能为空")
        Long bookId,

        @Size(max = 255, message = "备注不能超过 255 字符")
        String remark) {
}
