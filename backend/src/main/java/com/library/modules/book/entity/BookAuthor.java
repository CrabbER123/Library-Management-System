package com.library.modules.book.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 图书-作者关联（多对多，联合主键，无独立自增 ID）。
 */
@Data
@TableName("book_authors")
public class BookAuthor {

    private Long bookId;

    private Long authorId;
}
