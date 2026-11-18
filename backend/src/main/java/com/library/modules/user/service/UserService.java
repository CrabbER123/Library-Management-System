package com.library.modules.user.service;

import com.library.common.result.PageResult;
import com.library.modules.user.dto.RoleVO;
import com.library.modules.user.dto.UserCreateRequest;
import com.library.modules.user.dto.UserUpdateRequest;
import com.library.modules.user.dto.UserVO;

import java.util.List;

public interface UserService {

    PageResult<UserVO> list(int page, int size, String keyword, String role, String status, String sort);

    UserVO getById(Long id);

    UserVO create(UserCreateRequest request);

    UserVO update(Long id, UserUpdateRequest request);

    void updateStatus(Long id, String status);

    void updateRoles(Long id, List<String> roles);

    void delete(Long id);

    List<RoleVO> listRoles();
}
