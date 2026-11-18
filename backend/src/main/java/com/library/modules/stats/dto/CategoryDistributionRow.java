package com.library.modules.stats.dto;

import lombok.Data;

@Data
public class CategoryDistributionRow {
    private String categoryName;
    private Long bookCount;
}
