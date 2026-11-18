package com.library.modules.reader.controller;

import com.library.common.result.ApiResponse;
import com.library.common.result.PageResult;
import com.library.modules.reader.dto.ReaderRequest;
import com.library.modules.reader.dto.ReaderStatusRequest;
import com.library.modules.reader.dto.ReaderVO;
import com.library.modules.reader.service.ReaderService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 读者管理：增删改与列表仅管理员/馆员；读者本人可通过 /me 查看自己的档案。
 */
@RestController
@RequestMapping("/api/v1/readers")
@RequiredArgsConstructor
@Validated
@Tag(name = "读者管理")
public class ReaderController {

    private final ReaderService readerService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "分页查询读者")
    public ApiResponse<PageResult<ReaderVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 必须大于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 必须大于 0")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(readerService.list(page, size, keyword, status, sort));
    }

    @GetMapping("/me")
    @Operation(summary = "查询当前登录读者的档案")
    public ApiResponse<ReaderVO> me() {
        return ApiResponse.ok(readerService.getMine());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "查询读者详情")
    public ApiResponse<ReaderVO> get(@PathVariable Long id) {
        return ApiResponse.ok(readerService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "新增读者")
    public ApiResponse<ReaderVO> create(@Valid @RequestBody ReaderRequest request) {
        return ApiResponse.ok(readerService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "更新读者")
    public ApiResponse<ReaderVO> update(@PathVariable Long id, @Valid @RequestBody ReaderRequest request) {
        return ApiResponse.ok(readerService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "启用/停用读者")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody ReaderStatusRequest request) {
        readerService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "删除读者（存在借阅记录时拒绝）")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        readerService.delete(id);
        return ApiResponse.ok();
    }
}
