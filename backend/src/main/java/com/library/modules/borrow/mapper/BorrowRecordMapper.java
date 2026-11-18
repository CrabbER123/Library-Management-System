package com.library.modules.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.modules.borrow.entity.BorrowRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface BorrowRecordMapper extends BaseMapper<BorrowRecord> {

    /**
     * 读者当前在借数量。
     */
    @Select("SELECT COUNT(*) FROM borrow_records WHERE reader_id = #{readerId} AND status = 'BORROWED'")
    long countBorrowing(@Param("readerId") Long readerId);

    /**
     * 读者是否已借阅同一本书且尚未归还。
     */
    @Select("SELECT COUNT(*) FROM borrow_records WHERE reader_id = #{readerId} AND book_id = #{bookId} "
            + "AND status = 'BORROWED'")
    long countActiveBorrow(@Param("readerId") Long readerId, @Param("bookId") Long bookId);

    /**
     * 读者是否有过任意借阅记录（删除读者前检查）。
     */
    @Select("SELECT COUNT(*) FROM borrow_records WHERE reader_id = #{readerId}")
    long countByReader(@Param("readerId") Long readerId);
}
