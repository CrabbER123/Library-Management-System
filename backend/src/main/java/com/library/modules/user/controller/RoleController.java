package com.library.modules.user.controller;

import com.library.common.result.ApiResponse;
import com.library.modules.user.dto.RoleVO;
import com.library.modules.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色字典查询。
 */
@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "用户与角色（管理员）")
public class RoleController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "查询全部角色")
    public ApiResponse<List<RoleVO>> list() {
        return ApiResponse.ok(userService.listRoles());
    }
}
