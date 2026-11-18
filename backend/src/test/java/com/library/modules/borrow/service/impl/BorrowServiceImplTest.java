package com.library.modules.borrow.service.impl;

import com.library.common.exception.BizException;
import com.library.common.exception.ErrorCode;
import com.library.config.BorrowProperties;
import com.library.modules.book.entity.Book;
import com.library.modules.book.mapper.BookMapper;
import com.library.modules.borrow.dto.BorrowRecordVO;
import com.library.modules.borrow.dto.BorrowRequest;
import com.library.modules.borrow.entity.BorrowRecord;
import com.library.modules.borrow.mapper.BorrowRecordMapper;
import com.library.modules.reader.entity.Reader;
import com.library.modules.reader.mapper.ReaderMapper;
import com.library.security.AuthUser;
import com.library.support.MpTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BorrowServiceImplTest {

    private static final Long READER_ID = 1L;
    private static final Long BOOK_ID = 10L;
    private static final Long OPERATOR_ID = 2L;

    @Mock
    private BorrowRecordMapper borrowRecordMapper;
    @Mock
    private BookMapper bookMapper;
    @Mock
    private ReaderMapper readerMapper;

    private BorrowServiceImpl borrowService;

    @BeforeEach
    void setUp() {
        MpTestSupport.initTableInfo(BorrowRecord.class, Reader.class);
        BorrowProperties properties = new BorrowProperties(30, 2, 15, 5,
                new BigDecimal("0.50"), false, new BigDecimal("100.00"));
        borrowService = new BorrowServiceImpl(borrowRecordMapper, bookMapper, readerMapper, properties);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new AuthUser(OPERATOR_ID, "librarian", List.of("LIBRARIAN")), null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void borrowCreatesRecordAndDecrementsStock() {
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));
        when(bookMapper.selectById(BOOK_ID)).thenReturn(book("AVAILABLE", 3));
        when(borrowRecordMapper.countBorrowing(READER_ID)).thenReturn(2L);
        when(borrowRecordMapper.countActiveBorrow(READER_ID, BOOK_ID)).thenReturn(0L);
        when(bookMapper.decreaseAvailableStock(BOOK_ID)).thenReturn(1);
        when(borrowRecordMapper.insert(any(BorrowRecord.class))).thenAnswer(invocation -> {
            BorrowRecord record = invocation.getArgument(0);
            record.setId(99L);
            return 1;
        });
        when(readerMapper.selectBatchIds(any())).thenReturn(List.of(reader("ACTIVE", 5)));
        when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book("AVAILABLE", 3)));

        BorrowRecordVO vo = borrowService.borrow(new BorrowRequest(READER_ID, BOOK_ID, "柜台办理"));

        assertThat(vo.id()).isEqualTo(99L);
        assertThat(vo.status()).isEqualTo("BORROWED");
        assertThat(vo.borrowDate()).isEqualTo(LocalDate.now());
        assertThat(vo.dueDate()).isEqualTo(LocalDate.now().plusDays(30));
        assertThat(vo.readerName()).isEqualTo("张三");
        assertThat(vo.bookTitle()).isEqualTo("三体");
        assertThat(vo.currentFine()).isEqualByComparingTo("0.00");
        verify(bookMapper).decreaseAvailableStock(BOOK_ID);

        ArgumentCaptor<BorrowRecord> captor = ArgumentCaptor.forClass(BorrowRecord.class);
        verify(borrowRecordMapper).insert(captor.capture());
        BorrowRecord saved = captor.getValue();
        assertThat(saved.getOperatorId()).isEqualTo(OPERATOR_ID);
        assertThat(saved.getRenewTimes()).isZero();
        assertThat(saved.getFineAmount()).isEqualByComparingTo("0.00");
    }

    @Test
    void borrowRejectsSuspendedReader() {
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("SUSPENDED", 5));

        assertThatThrownBy(() -> borrowService.borrow(new BorrowRequest(READER_ID, BOOK_ID, null)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.READER_INACTIVE));
        verifyNoInteractions(bookMapper);
    }

    @Test
    void borrowRejectsWhenBorrowLimitReached() {
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));
        when(bookMapper.selectById(BOOK_ID)).thenReturn(book("AVAILABLE", 3));
        when(borrowRecordMapper.countBorrowing(READER_ID)).thenReturn(5L);

        assertThatThrownBy(() -> borrowService.borrow(new BorrowRequest(READER_ID, BOOK_ID, null)))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BORROW_LIMIT_EXCEEDED);
                    assertThat(ex.getMessage()).contains("上限 5");
                });
        verify(bookMapper, never()).decreaseAvailableStock(any());
    }

    @Test
    void borrowRejectsDuplicateActiveBorrow() {
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));
        when(bookMapper.selectById(BOOK_ID)).thenReturn(book("AVAILABLE", 3));
        when(borrowRecordMapper.countBorrowing(READER_ID)).thenReturn(0L);
        when(borrowRecordMapper.countActiveBorrow(READER_ID, BOOK_ID)).thenReturn(1L);

        assertThatThrownBy(() -> borrowService.borrow(new BorrowRequest(READER_ID, BOOK_ID, null)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE));
        verify(bookMapper, never()).decreaseAvailableStock(any());
    }

    @Test
    void borrowRejectsWhenStockIsExhausted() {
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));
        when(bookMapper.selectById(BOOK_ID)).thenReturn(book("AVAILABLE", 0));
        when(borrowRecordMapper.countBorrowing(READER_ID)).thenReturn(0L);
        when(borrowRecordMapper.countActiveBorrow(READER_ID, BOOK_ID)).thenReturn(0L);
        when(bookMapper.decreaseAvailableStock(BOOK_ID)).thenReturn(0);

        assertThatThrownBy(() -> borrowService.borrow(new BorrowRequest(READER_ID, BOOK_ID, null)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.STOCK_NOT_ENOUGH));
        verify(borrowRecordMapper, never()).insert(any(BorrowRecord.class));
    }

    @Test
    void borrowRejectsOfflineBook() {
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));
        when(bookMapper.selectById(BOOK_ID)).thenReturn(book("OFFLINE", 3));

        assertThatThrownBy(() -> borrowService.borrow(new BorrowRequest(READER_ID, BOOK_ID, null)))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BOOK_NOT_AVAILABLE));
    }

    @Test
    void returnSettlesFineForOverdueDays() {
        BorrowRecord record = borrowed(LocalDate.now().minusDays(10), 0);
        when(borrowRecordMapper.selectById(1L)).thenReturn(record);
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));
        when(readerMapper.selectBatchIds(any())).thenReturn(List.of(reader("ACTIVE", 5)));
        when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book("AVAILABLE", 3)));

        BorrowRecordVO vo = borrowService.returnBook(1L);

        assertThat(vo.status()).isEqualTo("RETURNED");
        assertThat(vo.returnDate()).isEqualTo(LocalDate.now());
        assertThat(vo.fineAmount()).isEqualByComparingTo("5.00");
        assertThat(vo.overdueDays()).isEqualTo(10);
        assertThat(vo.currentFine()).isEqualByComparingTo("5.00");
        verify(bookMapper).increaseAvailableStock(BOOK_ID);
    }

    @Test
    void returnOnTimeHasZeroFine() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(borrowed(LocalDate.now().plusDays(3), 0));
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));
        when(readerMapper.selectBatchIds(any())).thenReturn(List.of(reader("ACTIVE", 5)));
        when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book("AVAILABLE", 3)));

        BorrowRecordVO vo = borrowService.returnBook(1L);

        assertThat(vo.fineAmount()).isEqualByComparingTo("0.00");
        assertThat(vo.overdueDays()).isZero();
    }

    @Test
    void returnRejectsAlreadyReturnedRecord() {
        BorrowRecord record = borrowed(LocalDate.now().plusDays(1), 0);
        record.setStatus("RETURNED");
        record.setReturnDate(LocalDate.now());
        when(borrowRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> borrowService.returnBook(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
        verify(bookMapper, never()).increaseAvailableStock(any());
    }

    @Test
    void returnRejectsSuspendedReader() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(borrowed(LocalDate.now().plusDays(5), 0));
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("SUSPENDED", 5));

        assertThatThrownBy(() -> borrowService.returnBook(1L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.READER_INACTIVE);
                    assertThat(ex.getMessage()).contains("停用");
                });
        verify(borrowRecordMapper, never()).updateById(any(BorrowRecord.class));
        verify(bookMapper, never()).increaseAvailableStock(any());
    }

    @Test
    void renewExtendsDueDateAndCountsTimes() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(borrowed(LocalDate.now().plusDays(5), 1));
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));
        when(readerMapper.selectBatchIds(any())).thenReturn(List.of(reader("ACTIVE", 5)));
        when(bookMapper.selectBatchIds(any())).thenReturn(List.of(book("AVAILABLE", 3)));

        BorrowRecordVO vo = borrowService.renew(1L);

        assertThat(vo.dueDate()).isEqualTo(LocalDate.now().plusDays(20));
        assertThat(vo.renewTimes()).isEqualTo(2);
        verify(borrowRecordMapper).updateById(any(BorrowRecord.class));
    }

    @Test
    void renewRejectsOverdueRecord() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(borrowed(LocalDate.now().minusDays(1), 0));
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));

        assertThatThrownBy(() -> borrowService.renew(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RENEW_OVERDUE));
        verify(borrowRecordMapper, never()).updateById(any(BorrowRecord.class));
    }

    @Test
    void renewRejectsWhenRenewLimitReached() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(borrowed(LocalDate.now().plusDays(5), 2));
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("ACTIVE", 5));

        assertThatThrownBy(() -> borrowService.renew(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.RENEW_LIMIT_EXCEEDED));
    }

    @Test
    void renewRejectsReturnedRecord() {
        BorrowRecord record = borrowed(LocalDate.now().plusDays(5), 0);
        record.setStatus("RETURNED");
        when(borrowRecordMapper.selectById(1L)).thenReturn(record);

        assertThatThrownBy(() -> borrowService.renew(1L))
                .isInstanceOfSatisfying(BizException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.CONFLICT));
    }

    @Test
    void renewRejectsSuspendedReader() {
        when(borrowRecordMapper.selectById(1L)).thenReturn(borrowed(LocalDate.now().plusDays(5), 0));
        when(readerMapper.selectById(READER_ID)).thenReturn(reader("SUSPENDED", 5));

        assertThatThrownBy(() -> borrowService.renew(1L))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.READER_INACTIVE);
                    assertThat(ex.getMessage()).contains("停用");
                });
        verify(borrowRecordMapper, never()).updateById(any(BorrowRecord.class));
    }

    @Test
    void myListRequiresReaderProfile() {
        when(readerMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> borrowService.myList(1, 10, null))
                .isInstanceOfSatisfying(BizException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.NOT_FOUND);
                    assertThat(ex.getMessage()).contains("未绑定");
                });
    }

    // ---------------------------------------------------------------- 测试夹具

    private Reader reader(String status, int maxBorrow) {
        Reader reader = new Reader();
        reader.setId(READER_ID);
        reader.setName("张三");
        reader.setCardNo("R2024001");
        reader.setStatus(status);
        reader.setMaxBorrow(maxBorrow);
        return reader;
    }

    private Book book(String status, int availableStock) {
        Book book = new Book();
        book.setId(BOOK_ID);
        book.setTitle("三体");
        book.setIsbn("9787536692930");
        book.setStatus(status);
        book.setTotalStock(3);
        book.setAvailableStock(availableStock);
        return book;
    }

    private BorrowRecord borrowed(LocalDate dueDate, int renewTimes) {
        BorrowRecord record = new BorrowRecord();
        record.setId(1L);
        record.setReaderId(READER_ID);
        record.setBookId(BOOK_ID);
        record.setBorrowDate(dueDate.minusDays(30));
        record.setDueDate(dueDate);
        record.setRenewTimes(renewTimes);
        record.setStatus("BORROWED");
        record.setFineAmount(BigDecimal.ZERO.setScale(2));
        record.setOperatorId(OPERATOR_ID);
        return record;
    }
}
