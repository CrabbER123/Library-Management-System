package com.library.modules.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.query.Sorts;
import com.library.common.result.PageResult;
import com.library.modules.book.entity.Book;
import com.library.modules.book.mapper.BookMapper;
import com.library.modules.category.dto.CategoryRequest;
import com.library.modules.category.dto.CategoryVO;
import com.library.modules.category.entity.Category;
import com.library.modules.category.mapper.CategoryMapper;
import com.library.modules.category.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "name", "name",
            "code", "code",
            "createdAt", "created_at");

    private final CategoryMapper categoryMapper;
    private final BookMapper bookMapper;

    @Override
    public PageResult<CategoryVO> list(int page, int size, String keyword, String sort) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        QueryWrapper<Category> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("name", kw).or().like("code", kw).or().like("description", kw));
        }
        if (Sorts.apply(wrapper, sort, SORT_COLUMNS) == null) {
            Sorts.applyDefault(wrapper, "name", true);
        }

        Page<Category> result = categoryMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        return PageResult.of(result, this::toVO);
    }

    @Override
    public CategoryVO getById(Long id) {
        return toVO(requireCategory(id));
    }

    @Override
    @Transactional
    public CategoryVO create(CategoryRequest request) {
        String name = request.name().trim();
        ensureNameAvailable(name, null);

        Category category = new Category();
        category.setName(name);
        category.setCode(trimToNull(request.code()));
        category.setDescription(trimToNull(request.description()));
        categoryMapper.insert(category);
        return toVO(category);
    }

    @Override
    @Transactional
    public CategoryVO update(Long id, CategoryRequest request) {
        Category category = requireCategory(id);
        String name = request.name().trim();
        if (!name.equals(category.getName())) {
            ensureNameAvailable(name, id);
        }
        category.setName(name);
        category.setCode(trimToNull(request.code()));
        category.setDescription(trimToNull(request.description()));
        categoryMapper.updateById(category);
        return toVO(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireCategory(id);
        Long linked = bookMapper.selectCount(Wrappers.<Book>lambdaQuery().eq(Book::getCategoryId, id));
        if (linked != null && linked > 0) {
            throw new BizException(ErrorCode.CONFLICT,
                    "该分类下仍有 " + linked + " 本图书，请先调整图书分类");
        }
        categoryMapper.deleteById(id);
    }

    private void ensureNameAvailable(String name, Long excludeId) {
        Long count = categoryMapper.selectCount(Wrappers.<Category>lambdaQuery()
                .eq(Category::getName, name)
                .ne(excludeId != null, Category::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "分类已存在: " + name);
        }
    }

    private Category requireCategory(Long id) {
        Category category = categoryMapper.selectById(id);
        if (category == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "分类不存在: " + id);
        }
        return category;
    }

    private CategoryVO toVO(Category category) {
        return new CategoryVO(category.getId(), category.getName(), category.getCode(),
                category.getDescription(), category.getCreatedAt(), category.getUpdatedAt());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
