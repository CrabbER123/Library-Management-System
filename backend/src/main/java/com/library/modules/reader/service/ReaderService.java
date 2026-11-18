package com.library.modules.reader.service;

import com.library.common.result.PageResult;
import com.library.modules.reader.dto.ReaderRequest;
import com.library.modules.reader.dto.ReaderVO;

public interface ReaderService {

    PageResult<ReaderVO> list(int page, int size, String keyword, String status, String sort);

    ReaderVO getById(Long id);

    /**
     * 当前登录读者自己的档案。
     */
    ReaderVO getMine();

    ReaderVO create(ReaderRequest request);

    ReaderVO update(Long id, ReaderRequest request);

    void updateStatus(Long id, String status);

    /**
     * 删除读者；存在借阅记录时拒绝。
     */
    void delete(Long id);
}
