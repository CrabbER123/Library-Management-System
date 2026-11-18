package com.library.modules.book.controller;

import com.library.common.result.ApiResponse;
import com.library.common.result.PageResult;
import com.library.modules.book.dto.BookRequest;
import com.library.modules.book.dto.BookStatusRequest;
import com.library.modules.book.dto.BookStockRequest;
import com.library.modules.book.dto.BookVO;
import com.library.modules.book.service.BookService;
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
 * 图书管理：登录用户均可查询（读者也需检索图书），增删改仅管理员与馆员。
 */
@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
@Validated
@Tag(name = "图书管理")
public class BookController {

    private final BookService bookService;

    @GetMapping
    @Operation(summary = "分页搜索图书（关键字支持书名/副标题/ISBN/作者名）")
    public ApiResponse<PageResult<BookVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 必须大于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 必须大于 0")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long authorId,
            @RequestParam(required = false) Long publisherId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(bookService.list(page, size, keyword, categoryId, authorId, publisherId,
                status, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询图书详情")
    public ApiResponse<BookVO> get(@PathVariable Long id) {
        return ApiResponse.ok(bookService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "新增图书")
    public ApiResponse<BookVO> create(@Valid @RequestBody BookRequest request) {
        return ApiResponse.ok(bookService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "更新图书（可借库存按 总库存-已借出 自动重算）")
    public ApiResponse<BookVO> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return ApiResponse.ok(bookService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "上架/下架")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody BookStatusRequest request) {
        bookService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "调整总库存")
    public ApiResponse<BookVO> updateStock(@PathVariable Long id, @Valid @RequestBody BookStockRequest request) {
        return ApiResponse.ok(bookService.updateStock(id, request.totalStock()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "删除图书（存在借阅记录时拒绝）")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ApiResponse.ok();
    }
}
