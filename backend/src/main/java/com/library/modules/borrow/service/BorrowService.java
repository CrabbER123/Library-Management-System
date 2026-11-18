package com.library.modules.borrow.service;

import com.library.common.result.PageResult;
import com.library.modules.borrow.dto.BorrowRecordVO;
import com.library.modules.borrow.dto.BorrowRequest;

public interface BorrowService {

    /**
     * 借书：校验读者状态、借阅上限、重复借阅与库存后扣减库存并生成记录。
     */
    BorrowRecordVO borrow(BorrowRequest request);

    /**
     * 还书：结算逾期罚款、恢复库存。
     */
    BorrowRecordVO returnBook(Long id);

    /**
     * 续借：校验续借次数与逾期状态后延长应还日期。
     */
    BorrowRecordVO renew(Long id);

    PageResult<BorrowRecordVO> list(int page, int size, String keyword, String status, Long readerId,
                                    Long bookId, Boolean overdueOnly, String sort);

    BorrowRecordVO getById(Long id);

    /**
     * 读者本人借阅记录。
     */
    PageResult<BorrowRecordVO> myList(int page, int size, String status);
}
