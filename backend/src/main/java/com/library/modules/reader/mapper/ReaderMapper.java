package com.library.modules.reader.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.library.modules.reader.entity.Reader;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReaderMapper extends BaseMapper<Reader> {

    @Select("SELECT COUNT(*) FROM borrow_records WHERE reader_id = #{readerId}")
    long countBorrowRecords(@Param("readerId") Long readerId);

    /**
     * 批量统计在借数量（仅 BORROWED），用于列表页展示，避免逐行查询。
     */
    @Select("<script>SELECT reader_id AS readerId, COUNT(*) AS borrowCount FROM borrow_records "
            + "WHERE status = 'BORROWED' AND reader_id IN "
            + "<foreach collection='readerIds' item='id' open='(' separator=',' close=')'>#{id}</foreach> "
            + "GROUP BY reader_id</script>")
    List<ReaderBorrowCount> countBorrowingByReaderIds(@Param("readerIds") List<Long> readerIds);
}
