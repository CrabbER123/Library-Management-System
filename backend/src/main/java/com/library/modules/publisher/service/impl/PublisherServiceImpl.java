package com.library.modules.publisher.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.query.Sorts;
import com.library.common.result.PageResult;
import com.library.modules.book.entity.Book;
import com.library.modules.book.mapper.BookMapper;
import com.library.modules.publisher.dto.PublisherRequest;
import com.library.modules.publisher.dto.PublisherVO;
import com.library.modules.publisher.entity.Publisher;
import com.library.modules.publisher.mapper.PublisherMapper;
import com.library.modules.publisher.service.PublisherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PublisherServiceImpl implements PublisherService {

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "name", "name",
            "createdAt", "created_at");

    private final PublisherMapper publisherMapper;
    private final BookMapper bookMapper;

    @Override
    public PageResult<PublisherVO> list(int page, int size, String keyword, String sort) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        QueryWrapper<Publisher> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("name", kw).or().like("address", kw).or().like("phone", kw));
        }
        if (Sorts.apply(wrapper, sort, SORT_COLUMNS) == null) {
            Sorts.applyDefault(wrapper, "name", true);
        }

        Page<Publisher> result = publisherMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        return PageResult.of(result, this::toVO);
    }

    @Override
    public PublisherVO getById(Long id) {
        return toVO(requirePublisher(id));
    }

    @Override
    @Transactional
    public PublisherVO create(PublisherRequest request) {
        String name = request.name().trim();
        ensureNameAvailable(name, null);

        Publisher publisher = new Publisher();
        publisher.setName(name);
        publisher.setAddress(trimToNull(request.address()));
        publisher.setWebsite(trimToNull(request.website()));
        publisher.setPhone(trimToNull(request.phone()));
        publisherMapper.insert(publisher);
        return toVO(publisher);
    }

    @Override
    @Transactional
    public PublisherVO update(Long id, PublisherRequest request) {
        Publisher publisher = requirePublisher(id);
        String name = request.name().trim();
        if (!name.equals(publisher.getName())) {
            ensureNameAvailable(name, id);
        }
        publisher.setName(name);
        publisher.setAddress(trimToNull(request.address()));
        publisher.setWebsite(trimToNull(request.website()));
        publisher.setPhone(trimToNull(request.phone()));
        publisherMapper.updateById(publisher);
        return toVO(publisher);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requirePublisher(id);
        Long linked = bookMapper.selectCount(Wrappers.<Book>lambdaQuery().eq(Book::getPublisherId, id));
        if (linked != null && linked > 0) {
            throw new BizException(ErrorCode.CONFLICT,
                    "该出版社已关联 " + linked + " 本图书，请先调整图书的出版社");
        }
        publisherMapper.deleteById(id);
    }

    private void ensureNameAvailable(String name, Long excludeId) {
        Long count = publisherMapper.selectCount(Wrappers.<Publisher>lambdaQuery()
                .eq(Publisher::getName, name)
                .ne(excludeId != null, Publisher::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "出版社已存在: " + name);
        }
    }

    private Publisher requirePublisher(Long id) {
        Publisher publisher = publisherMapper.selectById(id);
        if (publisher == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "出版社不存在: " + id);
        }
        return publisher;
    }

    private PublisherVO toVO(Publisher publisher) {
        return new PublisherVO(publisher.getId(), publisher.getName(), publisher.getAddress(),
                publisher.getWebsite(), publisher.getPhone(), publisher.getCreatedAt(), publisher.getUpdatedAt());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
