package com.library.modules.reader.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.query.Sorts;
import com.library.common.result.PageResult;
import com.library.config.BorrowProperties;
import com.library.modules.reader.dto.ReaderRequest;
import com.library.modules.reader.dto.ReaderVO;
import com.library.modules.reader.entity.Reader;
import com.library.modules.reader.mapper.ReaderBorrowCount;
import com.library.modules.reader.mapper.ReaderMapper;
import com.library.modules.reader.service.ReaderService;
import com.library.modules.user.entity.User;
import com.library.modules.user.mapper.UserMapper;
import com.library.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReaderServiceImpl implements ReaderService {

    private static final Set<String> STATUSES = Set.of("ACTIVE", "SUSPENDED");
    private static final String DEFAULT_STATUS = "ACTIVE";

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "cardNo", "card_no",
            "name", "name",
            "createdAt", "created_at",
            "maxBorrow", "max_borrow");

    private final ReaderMapper readerMapper;
    private final UserMapper userMapper;
    private final BorrowProperties borrowProperties;

    @Override
    public PageResult<ReaderVO> list(int page, int size, String keyword, String status, String sort) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        QueryWrapper<Reader> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.trim();
            wrapper.and(w -> w.like("card_no", kw)
                    .or().like("name", kw)
                    .or().like("phone", kw)
                    .or().like("email", kw));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        if (Sorts.apply(wrapper, sort, SORT_COLUMNS) == null) {
            Sorts.applyDefault(wrapper, "id", false);
        }

        Page<Reader> result = readerMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        Map<Long, Long> borrowCounts = borrowCounts(result.getRecords().stream().map(Reader::getId).toList());
        return PageResult.of(result, reader -> toVO(reader, borrowCounts.getOrDefault(reader.getId(), 0L)));
    }

    @Override
    public ReaderVO getById(Long id) {
        Reader reader = requireReader(id);
        return toVO(reader, borrowCounts(List.of(id)).getOrDefault(id, 0L));
    }

    @Override
    public ReaderVO getMine() {
        Long userId = SecurityUtils.currentUserId();
        Reader reader = readerMapper.selectOne(Wrappers.<Reader>lambdaQuery().eq(Reader::getUserId, userId));
        if (reader == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "当前账号尚未绑定读者档案");
        }
        return toVO(reader, borrowCounts(List.of(reader.getId())).getOrDefault(reader.getId(), 0L));
    }

    @Override
    @Transactional
    public ReaderVO create(ReaderRequest request) {
        String cardNo = request.cardNo().trim();
        ensureCardNoAvailable(cardNo, null);
        validateUserBinding(request.userId(), null);

        Reader reader = new Reader();
        reader.setCardNo(cardNo);
        reader.setName(request.name().trim());
        reader.setPhone(trimToNull(request.phone()));
        reader.setEmail(trimToNull(request.email()));
        reader.setStatus(normalizeStatus(request.status()));
        reader.setMaxBorrow(request.maxBorrow() != null ? request.maxBorrow()
                : borrowProperties.maxBorrowPerReader());
        reader.setUserId(request.userId());
        readerMapper.insert(reader);
        return toVO(reader, 0L);
    }

    @Override
    @Transactional
    public ReaderVO update(Long id, ReaderRequest request) {
        Reader reader = requireReader(id);
        String cardNo = request.cardNo().trim();
        if (!cardNo.equals(reader.getCardNo())) {
            ensureCardNoAvailable(cardNo, id);
        }
        validateUserBinding(request.userId(), id);

        reader.setCardNo(cardNo);
        reader.setName(request.name().trim());
        reader.setPhone(trimToNull(request.phone()));
        reader.setEmail(trimToNull(request.email()));
        reader.setStatus(normalizeStatus(request.status()));
        reader.setMaxBorrow(request.maxBorrow() != null ? request.maxBorrow()
                : borrowProperties.maxBorrowPerReader());
        reader.setUserId(request.userId());
        readerMapper.updateById(reader);
        return toVO(reader, borrowCounts(List.of(id)).getOrDefault(id, 0L));
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        if (!STATUSES.contains(status)) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不支持的读者状态: " + status);
        }
        Reader reader = requireReader(id);
        reader.setStatus(status);
        readerMapper.updateById(reader);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireReader(id);
        long records = readerMapper.countBorrowRecords(id);
        if (records > 0) {
            throw new BizException(ErrorCode.CONFLICT,
                    "该读者已有 " + records + " 条借阅记录，不能删除；可将其状态改为停用");
        }
        readerMapper.deleteById(id);
    }

    // ---------------------------------------------------------------- 内部方法

    private Map<Long, Long> borrowCounts(List<Long> readerIds) {
        if (readerIds.isEmpty()) {
            return Map.of();
        }
        return readerMapper.countBorrowingByReaderIds(readerIds).stream()
                .filter(item -> item.getReaderId() != null && item.getBorrowCount() != null)
                .collect(Collectors.toMap(ReaderBorrowCount::getReaderId, ReaderBorrowCount::getBorrowCount,
                        (left, right) -> left, HashMap::new));
    }

    private void validateUserBinding(Long userId, Long excludeReaderId) {
        if (userId == null) {
            return;
        }
        if (userMapper.selectById(userId) == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "绑定的登录账号不存在: " + userId);
        }
        Long bound = readerMapper.selectCount(Wrappers.<Reader>lambdaQuery()
                .eq(Reader::getUserId, userId)
                .ne(excludeReaderId != null, Reader::getId, excludeReaderId));
        if (bound != null && bound > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "该登录账号已绑定其他读者档案");
        }
    }

    private void ensureCardNoAvailable(String cardNo, Long excludeId) {
        Long count = readerMapper.selectCount(Wrappers.<Reader>lambdaQuery()
                .eq(Reader::getCardNo, cardNo)
                .ne(excludeId != null, Reader::getId, excludeId));
        if (count != null && count > 0) {
            throw new BizException(ErrorCode.DUPLICATE, "借阅证号已存在: " + cardNo);
        }
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return DEFAULT_STATUS;
        }
        String value = status.trim().toUpperCase();
        if (!STATUSES.contains(value)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "不支持的读者状态: " + status);
        }
        return value;
    }

    private Reader requireReader(Long id) {
        Reader reader = readerMapper.selectById(id);
        if (reader == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "读者不存在: " + id);
        }
        return reader;
    }

    private ReaderVO toVO(Reader reader, long borrowingCount) {
        return new ReaderVO(reader.getId(), reader.getUserId(), reader.getCardNo(), reader.getName(),
                reader.getPhone(), reader.getEmail(), reader.getStatus(), reader.getMaxBorrow(),
                borrowingCount, reader.getCreatedAt(), reader.getUpdatedAt());
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
