package com.library.modules.author.dto;

import java.time.LocalDateTime;

public record AuthorVO(
        Long id,
        String name,
        String nationality,
        String intro,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
