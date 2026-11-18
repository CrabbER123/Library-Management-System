package com.library.modules.book.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BookStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "AVAILABLE|OFFLINE", message = "状态只能是 AVAILABLE 或 OFFLINE")
        String status) {
}
