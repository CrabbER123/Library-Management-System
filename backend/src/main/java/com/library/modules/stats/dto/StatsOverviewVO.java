package com.library.modules.stats.dto;

/** 首页概览指标 */
public record StatsOverviewVO(
        long totalTitles,
        long totalBooks,
        long availableStock,
        long borrowedCount,
        long overdueCount,
        long totalReaders,
        long activeReaders,
        long totalBorrows) {
}
