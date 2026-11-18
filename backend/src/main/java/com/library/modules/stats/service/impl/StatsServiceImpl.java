package com.library.modules.stats.service.impl;

import com.library.modules.stats.dto.CategoryDistributionRow;
import com.library.modules.stats.dto.CategoryDistributionVO;
import com.library.modules.stats.dto.PopularBookRow;
import com.library.modules.stats.dto.PopularBookVO;
import com.library.modules.stats.dto.StatsOverviewRow;
import com.library.modules.stats.dto.StatsOverviewVO;
import com.library.modules.stats.mapper.StatsMapper;
import com.library.modules.stats.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {

    private static final int MIN_LIMIT = 1;
    private static final int MAX_LIMIT = 50;

    private final StatsMapper statsMapper;

    @Override
    public StatsOverviewVO overview() {
        StatsOverviewRow row = statsMapper.selectOverview();
        if (row == null) {
            return new StatsOverviewVO(0, 0, 0, 0, 0, 0, 0, 0);
        }
        return new StatsOverviewVO(
                value(row.getTotalTitles()),
                value(row.getTotalBooks()),
                value(row.getAvailableStock()),
                value(row.getBorrowedCount()),
                value(row.getOverdueCount()),
                value(row.getTotalReaders()),
                value(row.getActiveReaders()),
                value(row.getTotalBorrows()));
    }

    @Override
    public List<PopularBookVO> popularBooks(int limit) {
        int safeLimit = Math.min(MAX_LIMIT, Math.max(MIN_LIMIT, limit));
        List<PopularBookRow> rows = statsMapper.selectPopularBooks(safeLimit);
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .map(row -> new PopularBookVO(
                        value(row.getBookId()),
                        row.getTitle(),
                        row.getIsbn(),
                        value(row.getBorrowCount())))
                .toList();
    }

    @Override
    public List<CategoryDistributionVO> categoryDistribution() {
        List<CategoryDistributionRow> rows = statsMapper.selectCategoryDistribution();
        if (rows == null) {
            return List.of();
        }
        return rows.stream()
                .filter(Objects::nonNull)
                .map(row -> new CategoryDistributionVO(row.getCategoryName(), value(row.getBookCount())))
                .toList();
    }

    private static long value(Long value) {
        return value == null ? 0L : value;
    }
}
