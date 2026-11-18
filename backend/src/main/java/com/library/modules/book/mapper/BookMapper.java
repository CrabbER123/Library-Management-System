package com.library.modules.book.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.modules.book.entity.Book;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface BookMapper extends BaseMapper<Book> {

    /**
     * 图书是否产生过借阅记录（存在则禁止删除，FK 亦为 RESTRICT）。
     */
    @Select("SELECT COUNT(*) FROM borrow_records WHERE book_id = #{bookId}")
    long countBorrowRecords(@Param("bookId") Long bookId);

    /**
     * 条件扣减可借库存：只有仍有库存且图书可借时才生效，返回 0 表示扣减失败（并发下已被借空）。
     */
    @Update("UPDATE books SET available_stock = available_stock - 1 "
            + "WHERE id = #{bookId} AND status = 'AVAILABLE' AND available_stock > 0")
    int decreaseAvailableStock(@Param("bookId") Long bookId);

    /**
     * 归还时恢复可借库存，重复归还由借阅记录状态拦截，这里再用 available_stock &lt; total_stock 兜底。
     */
    @Update("UPDATE books SET available_stock = available_stock + 1 "
            + "WHERE id = #{bookId} AND available_stock < total_stock")
    int increaseAvailableStock(@Param("bookId") Long bookId);
}
