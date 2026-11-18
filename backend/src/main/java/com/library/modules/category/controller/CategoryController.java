package com.library.modules.category.controller;

import com.library.common.result.ApiResponse;
import com.library.common.result.PageResult;
import com.library.modules.category.dto.CategoryRequest;
import com.library.modules.category.dto.CategoryVO;
import com.library.modules.category.service.CategoryService;
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
 * 分类管理：登录用户均可查询，增删改仅管理员与馆员。
 */
@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Validated
@Tag(name = "分类管理")
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    @Operation(summary = "分页查询分类")
    public ApiResponse<PageResult<CategoryVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 必须大于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 必须大于 0")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(categoryService.list(page, size, keyword, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询分类详情")
    public ApiResponse<CategoryVO> get(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "新增分类")
    public ApiResponse<CategoryVO> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "更新分类")
    public ApiResponse<CategoryVO> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "删除分类（存在关联图书时拒绝）")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.ok();
    }
}
