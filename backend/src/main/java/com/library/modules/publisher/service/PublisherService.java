package com.library.modules.publisher.service;

import com.library.common.result.PageResult;
import com.library.modules.publisher.dto.PublisherRequest;
import com.library.modules.publisher.dto.PublisherVO;

public interface PublisherService {

    PageResult<PublisherVO> list(int page, int size, String keyword, String sort);

    PublisherVO getById(Long id);

    PublisherVO create(PublisherRequest request);

    PublisherVO update(Long id, PublisherRequest request);

    /**
     * 删除出版社；存在关联图书时拒绝。
     */
    void delete(Long id);
}
