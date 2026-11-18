package com.library.modules.stats.dto;

import lombok.Data;

@Data
public class PopularBookRow {
    private Long bookId;
    private String title;
    private String isbn;
    private Long borrowCount;
}
