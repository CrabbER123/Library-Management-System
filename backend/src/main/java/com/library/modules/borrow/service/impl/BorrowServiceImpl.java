package com.library.modules.borrow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.common.query.Sorts;
import com.library.common.result.PageResult;
import com.library.common.constant.RoleCodes;
import com.library.config.BorrowProperties;
import com.library.modules.book.entity.Book;
import com.library.modules.book.mapper.BookMapper;
import com.library.modules.borrow.dto.BorrowRecordVO;
import com.library.modules.borrow.dto.BorrowRequest;
import com.library.modules.borrow.entity.BorrowRecord;
import com.library.modules.borrow.mapper.BorrowRecordMapper;
import com.library.modules.borrow.service.BorrowService;
import com.library.modules.reader.entity.Reader;
import com.library.modules.reader.mapper.ReaderMapper;
import com.library.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BorrowServiceImpl implements BorrowService {

    private static final String STATUS_BORROWED = "BORROWED";
    private static final String STATUS_RETURNED = "RETURNED";
    private static final Set<String> STATUSES = Set.of(STATUS_BORROWED, STATUS_RETURNED);
    private static final String BOOK_AVAILABLE = "AVAILABLE";
    private static final String READER_ACTIVE = "ACTIVE";

    private static final Map<String, String> SORT_COLUMNS = Map.of(
            "id", "id",
            "borrowDate", "borrow_date",
            "dueDate", "due_date",
            "returnDate", "return_date",
            "status", "status",
            "fineAmount", "fine_amount");

    private final BorrowRecordMapper borrowRecordMapper;
    private final BookMapper bookMapper;
    private final ReaderMapper readerMapper;
    private final BorrowProperties borrowProperties;

    @Override
    @Transactional
    public BorrowRecordVO borrow(BorrowRequest request) {
        // 读者状态是借阅资格的唯一入口：停用后借书、还书、续借都会被同一处拦截
        Long readerId = resolveBorrower(request.readerId());
        Reader reader = requireActiveReader(readerId, "借书");

        Book book = bookMapper.selectById(request.bookId());
        if (book == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "图书不存在: " + request.bookId());
        }
        if (!BOOK_AVAILABLE.equals(book.getStatus())) {
            throw new BizException(ErrorCode.BOOK_NOT_AVAILABLE, "该图书已下架，不能借出");
        }

        long borrowing = borrowRecordMapper.countBorrowing(reader.getId());
        int maxBorrow = reader.getMaxBorrow() != null ? reader.getMaxBorrow()
                : borrowProperties.maxBorrowPerReader();
        if (borrowing >= maxBorrow) {
            throw new BizException(ErrorCode.BORROW_LIMIT_EXCEEDED,
                    "该读者已借 " + borrowing + " 本，达到上限 " + maxBorrow + " 本");
        }
        if (borrowRecordMapper.countActiveBorrow(reader.getId(), book.getId()) > 0) {
            throw new BizException(ErrorCode.BOOK_NOT_AVAILABLE, "该读者已借阅此书且尚未归还");
        }
        if (bookMapper.decreaseAvailableStock(book.getId()) == 0) {
            throw new BizException(ErrorCode.STOCK_NOT_ENOUGH, "该书暂无可借库存");
        }

        LocalDate today = LocalDate.now();
        BorrowRecord record = new BorrowRecord();
        record.setReaderId(reader.getId());
        record.setBookId(book.getId());
        record.setBorrowDate(today);
        record.setDueDate(today.plusDays(borrowProperties.loanDays()));
        record.setRenewTimes(0);
        record.setStatus(STATUS_BORROWED);
        record.setFineAmount(BigDecimal.ZERO.setScale(2));
        record.setOperatorId(SecurityUtils.currentUserId());
        record.setRemark(StringUtils.hasText(request.remark()) ? request.remark().trim() : null);
        try {
            borrowRecordMapper.insert(record);
        } catch (DuplicateKeyException ex) {
            // 并发下唯一索引 uk_active_borrow 兜底，事务回滚会同时撤销库存扣减
            throw new BizException(ErrorCode.BOOK_NOT_AVAILABLE, "该读者已借阅此书且尚未归还");
        }
        return toVOs(List.of(record)).get(0);
    }

    @Override
    @Transactional
    public BorrowRecordVO returnBook(Long id) {
        BorrowRecord record = requireRecord(id);
        if (!STATUS_BORROWED.equals(record.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "该借阅记录已归还，无需重复操作");
        }
        ensureBorrowRecordOwnership(record);
        requireActiveReader(record.getReaderId(), "归还（请先启用该读者）");
        LocalDate today = LocalDate.now();
        record.setReturnDate(today);
        record.setStatus(STATUS_RETURNED);
        record.setFineAmount(fineFor(record.getDueDate(), today));
        borrowRecordMapper.updateById(record);
        bookMapper.increaseAvailableStock(record.getBookId());
        return toVOs(List.of(record)).get(0);
    }

    @Override
    @Transactional
    public BorrowRecordVO renew(Long id) {
        BorrowRecord record = requireRecord(id);
        if (!STATUS_BORROWED.equals(record.getStatus())) {
            throw new BizException(ErrorCode.CONFLICT, "已归还的借阅记录不能续借");
        }
        ensureBorrowRecordOwnership(record);
        requireActiveReader(record.getReaderId(), "续借（请先启用该读者）");
        LocalDate today = LocalDate.now();
        if (record.getDueDate().isBefore(today) && !borrowProperties.allowRenewWhenOverdue()) {
            throw new BizException(ErrorCode.RENEW_OVERDUE, "图书已逾期，请先归还并缴纳罚款");
        }
        int renewTimes = record.getRenewTimes() == null ? 0 : record.getRenewTimes();
        if (renewTimes >= borrowProperties.maxRenewTimes()) {
            throw new BizException(ErrorCode.RENEW_LIMIT_EXCEEDED,
                    "续借次数已达上限 " + borrowProperties.maxRenewTimes() + " 次");
        }
        record.setRenewTimes(renewTimes + 1);
        record.setDueDate(record.getDueDate().plusDays(borrowProperties.renewDays()));
        borrowRecordMapper.updateById(record);
        return toVOs(List.of(record)).get(0);
    }

    @Override
    public PageResult<BorrowRecordVO> list(int page, int size, String keyword, String status, Long readerId,
                                           Long bookId, Boolean overdueOnly, String sort) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);

        QueryWrapper<BorrowRecord> wrapper = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            String like = "%" + keyword.trim() + "%";
            wrapper.and(w -> w.apply("reader_id IN (SELECT id FROM readers WHERE name LIKE {0} "
                            + "OR card_no LIKE {0})", like)
                    .or().apply("book_id IN (SELECT id FROM books WHERE title LIKE {0} "
                            + "OR isbn LIKE {0})", like));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq("status", status.trim());
        }
        if (readerId != null) {
            wrapper.eq("reader_id", readerId);
        }
        if (bookId != null) {
            wrapper.eq("book_id", bookId);
        }
        if (Boolean.TRUE.equals(overdueOnly)) {
            wrapper.eq("status", STATUS_BORROWED).lt("due_date", LocalDate.now());
        }
        if (Sorts.apply(wrapper, sort, SORT_COLUMNS) == null) {
            Sorts.applyDefault(wrapper, "id", false);
        }

        Page<BorrowRecord> result = borrowRecordMapper.selectPage(new Page<>(safePage, safeSize), wrapper);
        return PageResult.of(result, toVOs(result.getRecords()));
    }

    @Override
    public BorrowRecordVO getById(Long id) {
        return toVOs(List.of(requireRecord(id))).get(0);
    }

    @Override
    public PageResult<BorrowRecordVO> myList(int page, int size, String status) {
        return list(page, size, null, status, currentReaderId(), null, null, null);
    }

    // ---------------------------------------------------------------- 内部方法

    /**
     * 借书的读者身份：馆员/管理员代为办理（必须显式指定 readerId）；
     * 读者本人自助借书时忽略请求里的 readerId，强制使用自己的档案，避免越权替他人借书。
     */
    private Long resolveBorrower(Long requestedReaderId) {
        if (isStaff()) {
            if (requestedReaderId == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "馆员办理借书时必须指定读者");
            }
            return requestedReaderId;
        }
        Long ownReaderId = currentReaderId();
        if (requestedReaderId != null && !ownReaderId.equals(requestedReaderId)) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能为自己办理借阅");
        }
        return ownReaderId;
    }

    /**
     * 读者只能操作自己的借阅记录；馆员/管理员不受限制。
     */
    private void ensureBorrowRecordOwnership(BorrowRecord record) {
        if (isStaff()) {
            return;
        }
        if (!currentReaderId().equals(record.getReaderId())) {
            throw new BizException(ErrorCode.FORBIDDEN, "只能操作自己的借阅记录");
        }
    }

    private boolean isStaff() {
        return SecurityUtils.hasRole(RoleCodes.ADMIN) || SecurityUtils.hasRole(RoleCodes.LIBRARIAN);
    }

    private Long currentReaderId() {
        Reader reader = readerMapper.selectOne(Wrappers.<Reader>lambdaQuery()
                .eq(Reader::getUserId, SecurityUtils.currentUserId()));
        if (reader == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "当前账号尚未绑定读者档案");
        }
        return reader.getId();
    }

    private Reader requireActiveReader(Long readerId, String action) {
        Reader reader = readerMapper.selectById(readerId);
        if (reader == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "读者不存在");
        }
        if (!READER_ACTIVE.equals(reader.getStatus())) {
            throw new BizException(ErrorCode.READER_INACTIVE, "该读者已被停用，不能" + action);
        }
        return reader;
    }

    private BorrowRecord requireRecord(Long id) {
        BorrowRecord record = borrowRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "借阅记录不存在: " + id);
        }
        return record;
    }

    /**
     * 逾期罚款 = 逾期天数 × 每日罚款；配置了封顶金额（&gt; 0）时取较小值。
     */
    private BigDecimal fineFor(LocalDate dueDate, LocalDate endDate) {
        long days = ChronoUnit.DAYS.between(dueDate, endDate);
        if (days <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        BigDecimal fine = borrowProperties.finePerDay().multiply(BigDecimal.valueOf(days));
        BigDecimal cap = borrowProperties.maxFineAmount();
        if (cap != null && cap.compareTo(BigDecimal.ZERO) > 0 && fine.compareTo(cap) > 0) {
            fine = cap;
        }
        return fine.setScale(2, RoundingMode.HALF_UP);
    }

    private List<BorrowRecordVO> toVOs(List<BorrowRecord> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        Map<Long, Reader> readers = loadByIds(records.stream().map(BorrowRecord::getReaderId).toList(),
                readerMapper::selectBatchIds, Reader::getId);
        Map<Long, Book> books = loadByIds(records.stream().map(BorrowRecord::getBookId).toList(),
                bookMapper::selectBatchIds, Book::getId);

        LocalDate today = LocalDate.now();
        return records.stream().map(record -> {
            Reader reader = readers.get(record.getReaderId());
            Book book = books.get(record.getBookId());
            LocalDate endDate = STATUS_RETURNED.equals(record.getStatus()) && record.getReturnDate() != null
                    ? record.getReturnDate() : today;
            long overdueDays = Math.max(0, ChronoUnit.DAYS.between(record.getDueDate(), endDate));
            BigDecimal currentFine = STATUS_RETURNED.equals(record.getStatus())
                    ? record.getFineAmount() : fineFor(record.getDueDate(), today);
            return new BorrowRecordVO(record.getId(), record.getReaderId(),
                    reader != null ? reader.getName() : null,
                    reader != null ? reader.getCardNo() : null,
                    record.getBookId(),
                    book != null ? book.getTitle() : null,
                    book != null ? book.getIsbn() : null,
                    record.getBorrowDate(), record.getDueDate(), record.getReturnDate(),
                    record.getRenewTimes(), record.getStatus(), record.getFineAmount(),
                    overdueDays, currentFine, record.getOperatorId(), record.getCreatedAt());
        }).toList();
    }

    private <T> Map<Long, T> loadByIds(List<Long> ids, Function<Set<Long>, List<T>> loader,
                                       Function<T, Long> idGetter) {
        Set<Long> distinct = ids.stream().filter(java.util.Objects::nonNull)
                .collect(Collectors.toCollection(java.util.HashSet::new));
        if (distinct.isEmpty()) {
            return new HashMap<>();
        }
        return loader.apply(distinct).stream()
                .collect(Collectors.toMap(idGetter, Function.identity(), (left, right) -> left, HashMap::new));
    }
}
