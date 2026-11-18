package com.library.modules.stats.service;

import com.library.modules.stats.dto.CategoryDistributionVO;
import com.library.modules.stats.dto.PopularBookVO;
import com.library.modules.stats.dto.StatsOverviewVO;

import java.util.List;

public interface StatsService {

    StatsOverviewVO overview();

    List<PopularBookVO> popularBooks(int limit);

    List<CategoryDistributionVO> categoryDistribution();
}
