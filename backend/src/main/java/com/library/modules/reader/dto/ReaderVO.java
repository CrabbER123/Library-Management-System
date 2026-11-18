package com.library.modules.reader.dto;

import java.time.LocalDateTime;

public record ReaderVO(
        Long id,
        Long userId,
        String cardNo,
        String name,
        String phone,
        String email,
        String status,
        Integer maxBorrow,
        Long borrowingCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
