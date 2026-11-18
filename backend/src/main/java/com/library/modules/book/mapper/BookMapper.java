package com.library.modules.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.modules.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 图书是否产生过借阅记录（存在则禁止删除，FK 亦为 RESTRICT）。
     */
    @Select("SELECT COUNT(*) FROM borrow_records WHERE book_id = #{bookId}")
    long countBorrowRecords(@Param("bookId") Long bookId);
}
