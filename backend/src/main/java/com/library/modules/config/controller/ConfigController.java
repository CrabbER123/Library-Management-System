package com.library.modules.config.controller;

import com.library.common.result.ApiResponse;
import com.library.config.BorrowProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 只读系统配置：前端据此展示借期、续借、罚款规则，避免规则硬编码在前端。
 */
@RestController
@RequestMapping("/api/v1/configs")
@RequiredArgsConstructor
@Tag(name = "系统配置")
public class ConfigController {

    private final BorrowProperties borrowProperties;

    @GetMapping("/borrow-rules")
    @Operation(summary = "查询借阅规则")
    public ApiResponse<BorrowProperties> borrowRules() {
        return ApiResponse.ok(borrowProperties);
    }
}
