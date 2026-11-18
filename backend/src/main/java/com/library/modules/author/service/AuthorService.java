package com.library.modules.author.service;

import com.library.common.result.PageResult;
import com.library.modules.author.dto.AuthorRequest;
import com.library.modules.author.dto.AuthorVO;

public interface AuthorService {

    PageResult<AuthorVO> list(int page, int size, String keyword, String sort);

    AuthorVO getById(Long id);

    AuthorVO create(AuthorRequest request);

    AuthorVO update(Long id, AuthorRequest request);

    /**
     * 删除作者；存在关联图书时拒绝（避免借阅数据的作者信息丢失）。
     */
    void delete(Long id);
}
