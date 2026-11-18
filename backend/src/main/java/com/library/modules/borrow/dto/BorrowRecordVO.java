package com.library.modules.borrow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借阅记录视图。
 *
 * @param overdueDays 逾期天数：在借记录按今天计算，已还记录按归还日计算；未逾期为 0
 * @param currentFine 当前应缴罚款：在借记录为按今天试算的金额，已还记录为归还时结算金额
 */
public record BorrowRecordVO(
        Long id,
        Long readerId,
        String readerName,
        String readerCardNo,
        Long bookId,
        String bookTitle,
        String bookIsbn,
        LocalDate borrowDate,
        LocalDate dueDate,
        LocalDate returnDate,
        Integer renewTimes,
        String status,
        BigDecimal fineAmount,
        long overdueDays,
        BigDecimal currentFine,
        Long operatorId,
        LocalDateTime createdAt) {
}
