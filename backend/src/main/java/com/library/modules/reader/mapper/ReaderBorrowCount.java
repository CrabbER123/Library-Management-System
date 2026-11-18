package com.library.modules.reader.mapper;

import lombok.Data;

/**
 * 读者在借数量聚合结果。
 */
@Data
public class ReaderBorrowCount {

    private Long readerId;

    private Long borrowCount;
}
