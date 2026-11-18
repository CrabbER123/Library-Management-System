package com.library.modules.reader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReaderStatusRequest(
        @NotBlank(message = "状态不能为空")
        @Pattern(regexp = "ACTIVE|SUSPENDED", message = "状态只能是 ACTIVE 或 SUSPENDED")
        String status) {
}
