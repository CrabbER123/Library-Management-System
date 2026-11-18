package com.library.modules.category.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.modules.book.entity.Book;
import com.library.modules.book.mapper.BookMapper;
import com.library.modules.category.dto.CategoryRequest;
import com.library.modules.category.dto.CategoryVO;
import com.library.modules.category.entity.Category;
import com.library.modules.category.mapper.CategoryMapper;
import com.library.support.MpTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private BookMapper bookMapper;

    private CategoryServiceImpl categoryService;

    @BeforeEach
    void setUp() {
        MpTestSupport.initTableInfo(Category.class, Book.class);
        categoryService = new CategoryServiceImpl(categoryMapper, bookMapper);
    }

    @Test
    void listSortsByCodeAscendingByDefault() {
        String sql = captureListSql(null);

        assertThat(normalized(sql)).contains("order by code asc");
    }

    @Test
    void listKeepsExplicitAllowedSort() {
        String sql = captureListSql("name,asc");

        assertThat(normalized(sql)).contains("order by name asc").doesNotContain("code asc");
    }

    @Test
    void listFallsBackToDefaultWhenSortFieldIsNotAllowed() {
        String sql = captureListSql("description desc; drop table books");

        assertThat(normalized(sql)).contains("order by code asc");
    }

    @Test
    void listFiltersByKeywordOnNameCodeAndDescription() {
        String sql = captureListSql(null, "计算机");

        String normalizedSql = normalized(sql);
        assertThat(normalizedSql).contains("name like").contains("code like").contains("description like");
    }

    @Test
    void createTrimsInputAndClearsBlankFields() {
        when(categoryMapper.selectCount(any())).thenReturn(0L);
        when(categoryMapper.insert(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(9L);
            return 1;
        });

        CategoryVO created = categoryService.create(new CategoryRequest("  计算机 ", "  ", "  "));

        assertThat(created.id()).isEqualTo(9L);
        assertThat(created.name()).isEqualTo("计算机");
        assertThat(created.code()).isNull();
        assertThat(created.description()).isNull();
    }

    @Test
    void createRejectsDuplicateName() {
        when(categoryMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> categoryService.create(new CategoryRequest("计算机", "CS", null)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.DUPLICATE));
        verify(categoryMapper, never()).insert(any(Category.class));
    }

    @Test
    void deleteIsRejectedWhenBooksStillReferenceTheCategory() {
        when(categoryMapper.selectById(2L)).thenReturn(category(2L, "计算机"));
        when(bookMapper.selectCount(any())).thenReturn(3L);

        assertThatThrownBy(() -> categoryService.delete(2L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT);
                    assertThat(ex.getMessage()).contains("3");
                });
        verify(categoryMapper, never()).deleteById(2L);
    }

    @Test
    void deleteRemovesUnusedCategory() {
        when(categoryMapper.selectById(2L)).thenReturn(category(2L, "计算机"));
        when(bookMapper.selectCount(any())).thenReturn(0L);

        categoryService.delete(2L);

        verify(categoryMapper).deleteById(2L);
    }

    private String captureListSql(String sort) {
        return captureListSql(sort, null);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String captureListSql(String sort, String keyword) {
        ArgumentCaptor<Wrapper<Category>> captor = ArgumentCaptor.forClass((Class) Wrapper.class);
        when(categoryMapper.selectPage(any(), captor.capture()))
                .thenAnswer(invocation -> {
                    Page<Category> page = invocation.getArgument(0);
                    page.setRecords(List.of());
                    page.setTotal(0);
                    return page;
                });

        categoryService.list(1, 10, keyword, sort);

        return captor.getValue().getSqlSegment();
    }

    private String normalized(String sql) {
        return sql.replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private Category category(Long id, String name) {
        Category category = new Category();
        category.setId(id);
        category.setName(name);
        return category;
    }
}
