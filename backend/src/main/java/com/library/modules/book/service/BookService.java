package com.library.modules.book.service;

import com.library.common.result.PageResult;
import com.library.modules.book.dto.BookRequest;
import com.library.modules.book.dto.BookVO;

public interface BookService {

    PageResult<BookVO> list(int page, int size, String keyword, Long categoryId, Long authorId,
                            Long publisherId, String status, String sort);

    BookVO getById(Long id);

    BookVO create(BookRequest request);

    BookVO update(Long id, BookRequest request);

    void updateStatus(Long id, String status);

    /**
     * 调整总库存；新总库存不得小于已借出数量，可借库存随之重算。
     */
    BookVO updateStock(Long id, int totalStock);

    /**
     * 删除图书；存在借阅记录时拒绝（历史数据不可丢失），可改为下架。
     */
    void delete(Long id);
}
