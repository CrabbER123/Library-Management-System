package com.library.modules.book.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record BookVO(
        Long id,
        String isbn,
        String title,
        String subtitle,
        List<AuthorBrief> authors,
        Long publisherId,
        String publisherName,
        Long categoryId,
        String categoryName,
        LocalDate publishDate,
        String language,
        BigDecimal price,
        String coverUrl,
        String summary,
        Integer totalStock,
        Integer availableStock,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
