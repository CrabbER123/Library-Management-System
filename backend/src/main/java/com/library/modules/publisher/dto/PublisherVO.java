package com.library.modules.publisher.dto;

import java.time.LocalDateTime;

public record PublisherVO(
        Long id,
        String name,
        String address,
        String website,
        String phone,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
