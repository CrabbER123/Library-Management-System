package com.library.modules.category.service;

import com.library.common.result.PageResult;
import com.library.modules.category.dto.CategoryRequest;
import com.library.modules.category.dto.CategoryVO;

public interface CategoryService {

    PageResult<CategoryVO> list(int page, int size, String keyword, String sort);

    CategoryVO getById(Long id);

    CategoryVO create(CategoryRequest request);

    CategoryVO update(Long id, CategoryRequest request);

    /**
     * 删除分类；存在关联图书时拒绝。
     */
    void delete(Long id);
}
