package com.library.modules.publisher.controller;

import com.library.common.result.ApiResponse;
import com.library.common.result.PageResult;
import com.library.modules.publisher.dto.PublisherRequest;
import com.library.modules.publisher.dto.PublisherVO;
import com.library.modules.publisher.service.PublisherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 出版社管理：登录用户均可查询，增删改仅管理员与馆员。
 */
@RestController
@RequestMapping("/api/v1/publishers")
@RequiredArgsConstructor
@Validated
@Tag(name = "出版社管理")
public class PublisherController {

    private final PublisherService publisherService;

    @GetMapping
    @Operation(summary = "分页查询出版社")
    public ApiResponse<PageResult<PublisherVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 必须大于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 必须大于 0")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(publisherService.list(page, size, keyword, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询出版社详情")
    public ApiResponse<PublisherVO> get(@PathVariable Long id) {
        return ApiResponse.ok(publisherService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "新增出版社")
    public ApiResponse<PublisherVO> create(@Valid @RequestBody PublisherRequest request) {
        return ApiResponse.ok(publisherService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "更新出版社")
    public ApiResponse<PublisherVO> update(@PathVariable Long id, @Valid @RequestBody PublisherRequest request) {
        return ApiResponse.ok(publisherService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "删除出版社（存在关联图书时拒绝）")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        publisherService.delete(id);
        return ApiResponse.ok();
    }
}
