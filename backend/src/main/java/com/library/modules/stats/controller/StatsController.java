package com.library.modules.stats.controller;

import com.library.common.result.ApiResponse;
import com.library.modules.stats.dto.CategoryDistributionVO;
import com.library.modules.stats.dto.PopularBookVO;
import com.library.modules.stats.dto.StatsOverviewVO;
import com.library.modules.stats.service.StatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 运营统计：面向管理员与馆员 */
@Tag(name = "统计", description = "馆藏与借阅统计")
@RestController
@RequestMapping("/api/v1/stats")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
public class StatsController {

    private final StatsService statsService;

    @Operation(summary = "馆藏与借阅概览")
    @GetMapping("/overview")
    public ApiResponse<StatsOverviewVO> overview() {
        return ApiResponse.ok(statsService.overview());
    }

    @Operation(summary = "借阅排行榜")
    @GetMapping("/popular-books")
    public ApiResponse<List<PopularBookVO>> popularBooks(
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ApiResponse.ok(statsService.popularBooks(limit));
    }

    @Operation(summary = "分类馆藏分布")
    @GetMapping("/category-distribution")
    public ApiResponse<List<CategoryDistributionVO>> categoryDistribution() {
        return ApiResponse.ok(statsService.categoryDistribution());
    }
}
