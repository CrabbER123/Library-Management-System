package com.library.modules.author.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.query.Sorts;
import com.library.common.result.PageResult;
import com.library.modules.author.dto.AuthorRequest;
import com.library.modules.author.dto.AuthorVO;
import com.library.modules.author.entity.Author;
import com.library.modules.author.mapper.AuthorMapper;
import com.library.modules.author.service.AuthorService;
import com.library.modules.book.mapper.BookAuthorMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthorServiceImpl implements AuthorService {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "name", "name",
            "createdAt", "created_at");

    private final AuthorMapper authorMapper;
    private final BookAuthorMapper bookAuthorMapper;

    @Override
    public PageResult<AuthorVO> list(int page, int size, String keyword, String sort) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        QueryWrapper<Author> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("name", kw).or().like("nationality", kw));
        }
        if (Sorts.apply(wrapper, sort, SORT_COLUMNS) == null) {
            Sorts.applyDefault(wrapper, "name", true);
        }

        Page<Author> result = authorMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        return PageResult.of(result, this::toVO);
    }

    @Override
    public AuthorVO getById(Long id) {
        return toVO(requireAuthor(id));
    }

    @Override
    @Transactional
    public AuthorVO create(AuthorRequest request) {
        String name = request.name().trim();
        ensureNameAvailable(name, null);

        Author author = new Author();
        author.setName(name);
        author.setNationality(trimToNull(request.nationality()));
        author.setIntro(trimToNull(request.intro()));
        authorMapper.insert(author);
        return toVO(author);
    }

    @Override
    @Transactional
    public AuthorVO update(Long id, AuthorRequest request) {
        Author author = requireAuthor(id);
        String name = request.name().trim();
        if (!name.equals(author.getName())) {
            ensureNameAvailable(name, id);
        }
        author.setName(name);
        author.setNationality(trimToNull(request.nationality()));
        author.setIntro(trimToNull(request.intro()));
        authorMapper.updateById(author);
        return toVO(author);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireAuthor(id);
        long linked = bookAuthorMapper.countByAuthorId(id);
        if (linked > 0) {
            throw new BizException(ErrorCode.CONFLICT,
                    "该作者已关联 " + linked + " 本图书，请先在图书中移除该作者");
        }
        authorMapper.deleteById(id);
    }

    private void ensureNameAvailable(String name, Long excludeId) {
        Long count = authorMapper.selectCount(Wrappers.<Author>lambdaQuery()
                .eq(Author::getName, name)
                .ne(excludeId != null, Author::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "作者已存在: " + name);
        }
    }

    private Author requireAuthor(Long id) {
        Author author = authorMapper.selectById(id);
        if (author == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "作者不存在: " + id);
        }
        return author;
    }

    private AuthorVO toVO(Author author) {
        return new AuthorVO(author.getId(), author.getName(), author.getNationality(), author.getIntro(),
                author.getCreatedAt(), author.getUpdatedAt());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
