package com.library.modules.category.dto;

import java.time.LocalDateTime;

public record CategoryVO(
        Long id,
        String name,
        String code,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
