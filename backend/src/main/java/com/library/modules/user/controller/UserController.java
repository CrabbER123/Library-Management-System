package com.library.modules.user.controller;

import com.library.common.result.ApiResponse;
import com.library.common.result.PageResult;
import com.library.modules.user.dto.UserCreateRequest;
import com.library.modules.user.dto.UserRolesRequest;
import com.library.modules.user.dto.UserStatusRequest;
import com.library.modules.user.dto.UserUpdateRequest;
import com.library.modules.user.dto.UserVO;
import com.library.modules.user.service.UserService;
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
 * 账号管理，仅管理员可用。
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "用户与角色（管理员）")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "分页查询账号")
    public ApiResponse<PageResult<UserVO>> list(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "page 必须大于 0") int page,
            @RequestParam(defaultValue = "10") @Min(value = 1, message = "size 必须大于 0")
            @Max(value = 100, message = "size 不能超过 100") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sort) {
        return ApiResponse.ok(userService.list(page, size, keyword, role, status, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询账号详情")
    public ApiResponse<UserVO> get(@PathVariable Long id) {
        return ApiResponse.ok(userService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "创建账号")
    public ApiResponse<UserVO> create(@Valid @RequestBody UserCreateRequest request) {
        return ApiResponse.ok(userService.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新账号资料（用户名不可修改）")
    public ApiResponse<UserVO> update(@PathVariable Long id, @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.ok(userService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用账号")
    public ApiResponse<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody UserStatusRequest request) {
        userService.updateStatus(id, request.status());
        return ApiResponse.ok();
    }

    @PutMapping("/{id}/roles")
    @Operation(summary = "更新账号角色（全量替换）")
    public ApiResponse<Void> updateRoles(@PathVariable Long id, @Valid @RequestBody UserRolesRequest request) {
        userService.updateRoles(id, request.roles());
        return ApiResponse.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除账号")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.ok();
    }
}
