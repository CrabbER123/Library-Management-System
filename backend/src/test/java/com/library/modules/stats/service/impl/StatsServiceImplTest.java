package com.library.modules.stats.service.impl;

import com.library.modules.stats.dto.CategoryDistributionRow;
import com.library.modules.stats.dto.PopularBookRow;
import com.library.modules.stats.dto.PopularBookVO;
import com.library.modules.stats.dto.StatsOverviewRow;
import com.library.modules.stats.mapper.StatsMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatsServiceImplTest {

    @Mock
    private StatsMapper statsMapper;

    private StatsServiceImpl statsService;

    @BeforeEach
    void setUp() {
        statsService = new StatsServiceImpl(statsMapper);
    }

    @Test
    @DisplayName("概览：把聚合行映射为 VO")
    void overview_mapsRowToVo() {
        StatsOverviewRow row = new StatsOverviewRow();
        row.setTotalTitles(8L);
        row.setTotalBooks(37L);
        row.setAvailableStock(35L);
        row.setBorrowedCount(2L);
        row.setOverdueCount(1L);
        row.setTotalReaders(3L);
        row.setActiveReaders(3L);
        row.setTotalBorrows(6L);
        when(statsMapper.selectOverview()).thenReturn(row);

        var vo = statsService.overview();

        assertThat(vo.totalTitles()).isEqualTo(8);
        assertThat(vo.totalBooks()).isEqualTo(37);
        assertThat(vo.availableStock()).isEqualTo(35);
        assertThat(vo.borrowedCount()).isEqualTo(2);
        assertThat(vo.overdueCount()).isEqualTo(1);
        assertThat(vo.totalReaders()).isEqualTo(3);
        assertThat(vo.activeReaders()).isEqualTo(3);
        assertThat(vo.totalBorrows()).isEqualTo(6);
    }

    @Test
    @DisplayName("概览：空结果或空字段一律返回 0")
    void overview_handlesNulls() {
        StatsOverviewRow row = new StatsOverviewRow();
        when(statsMapper.selectOverview()).thenReturn(row);

        var vo = statsService.overview();
        assertThat(vo.totalTitles()).isZero();
        assertThat(vo.totalBorrows()).isZero();

        when(statsMapper.selectOverview()).thenReturn(null);
        assertThat(statsService.overview().availableStock()).isZero();
    }

    @Test
    @DisplayName("借阅排行：limit 超界时收敛到 1..50")
    void popularBooks_clampsLimit() {
        when(statsMapper.selectPopularBooks(anyInt())).thenReturn(Collections.emptyList());

        statsService.popularBooks(0);
        statsService.popularBooks(999);
        statsService.popularBooks(-5);

        ArgumentCaptor<Integer> captor = ArgumentCaptor.forClass(Integer.class);
        verify(statsMapper, org.mockito.Mockito.times(3)).selectPopularBooks(captor.capture());
        assertThat(captor.getAllValues()).containsExactly(1, 50, 1);
    }

    @Test
    @DisplayName("借阅排行：过滤空行并把空计数归零")
    void popularBooks_mapsRows() {
        PopularBookRow first = new PopularBookRow();
        first.setBookId(2L);
        first.setTitle("三体");
        first.setIsbn("9787536692930");
        first.setBorrowCount(3L);

        PopularBookRow second = new PopularBookRow();
        second.setBookId(5L);
        second.setTitle("人类简史");

        when(statsMapper.selectPopularBooks(10)).thenReturn(Arrays.asList(first, second, null));

        List<PopularBookVO> result = statsService.popularBooks(10);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).bookId()).isEqualTo(2);
        assertThat(result.get(0).borrowCount()).isEqualTo(3);
        assertThat(result.get(1).borrowCount()).isZero();
    }

    @Test
    @DisplayName("分类分布：空列表安全返回空集合")
    void categoryDistribution_handlesNullList() {
        when(statsMapper.selectCategoryDistribution()).thenReturn(null);
        assertThat(statsService.categoryDistribution()).isEmpty();
    }

    @Test
    @DisplayName("分类分布：映射分类名与馆藏种数")
    void categoryDistribution_mapsRows() {
        CategoryDistributionRow lit = new CategoryDistributionRow();
        lit.setCategoryName("文学");
        lit.setBookCount(4L);

        CategoryDistributionRow empty = new CategoryDistributionRow();
        empty.setCategoryName("少儿");
        empty.setBookCount(null);

        when(statsMapper.selectCategoryDistribution()).thenReturn(Arrays.asList(lit, empty));

        var result = statsService.categoryDistribution();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).categoryName()).isEqualTo("文学");
        assertThat(result.get(0).bookCount()).isEqualTo(4);
        assertThat(result.get(1).bookCount()).isZero();
    }
}
