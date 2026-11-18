package com.library.modules.stats.dto;

import lombok.Data;

/** 概览聚合的查询行：MyBatis 通过 setter 映射，对外只暴露不可变 VO */
@Data
public class StatsOverviewRow {
    private Long totalTitles;
    private Long totalBooks;
    private Long availableStock;
    private Long borrowedCount;
    private Long overdueCount;
    private Long totalReaders;
    private Long activeReaders;
    private Long totalBorrows;
}
