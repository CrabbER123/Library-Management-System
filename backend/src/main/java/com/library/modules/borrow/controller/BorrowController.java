package com.library.modules.borrow.controller;

import com.library.common.result.ApiResponse;
import com.library.common.result.PageResult;
import com.library.modules.borrow.dto.BorrowRecordVO;
import com.library.modules.borrow.dto.BorrowRequest;
import com.library.modules.borrow.service.BorrowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 借阅管理：借书/还书/续借由管理员或馆员在柜台办理；读者只能查看自己的记录。
 */
@RestController
@RequestMapping("/api/v1/borrows")
@RequiredArgsConstructor
@Validated
@Tag(name = "借阅管理")
public class BorrowController {

    private final BorrowService borrowService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "借书（馆员可为任意读者办理，读者只能为自己借书）")
    public ApiResponse<BorrowRecordVO> borrow(@Valid @RequestBody BorrowRequest request) {
        return ApiResponse.ok(borrowService.borrow(request));
    }

    @PostMapping("/{id}/return")
    @Operation(summary = "还书（自动结算逾期罚款；读者只能归还自己的记录）")
    public ApiResponse<BorrowRecordVO> returnBook(@PathVariable Long id) {
        return ApiResponse.ok(borrowService.returnBook(id));
    }

    @PostMapping("/{id}/renew")
    @Operation(summary = "续借（读者只能续借自己的记录）")
    public ApiResponse<BorrowRecordVO> renew(@PathVariable Long id) {
        return ApiResponse.ok(borrowService.renew(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "分页查询借阅记录")
    public ApiResponse<PageResult<BorrowRecordVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 必须大于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 必须大于 0")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long readerId,
            @RequestParam(required = false) Long bookId,
            @RequestParam(required = false) Boolean overdueOnly,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(borrowService.list(page, size, keyword, status, readerId, bookId, overdueOnly, sort));
    }

    @GetMapping("/my")
    @Operation(summary = "我的借阅记录")
    public ApiResponse<PageResult<BorrowRecordVO>> myList(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 必须大于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 必须大于 0")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) String status) {
        return ApiResponse.ok(borrowService.myList(page, size, status));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','LIBRARIAN')")
    @Operation(summary = "借阅记录详情")
    public ApiResponse<BorrowRecordVO> get(@PathVariable Long id) {
        return ApiResponse.ok(borrowService.getById(id));
    }
}
