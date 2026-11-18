package com.library.modules.book.mapper;

import com.library.modules.book.entity.BookAuthor;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * book_authors 关联表操作。批量查询每个书的作者，避免逐条查询造成 N+1。
 */
@Mapper
public interface BookAuthorMapper {

    @Select("SELECT COUNT(*) FROM book_authors WHERE author_id = #{authorId}")
    long countByAuthorId(@Param("authorId") Long authorId);

    @Select("<script>SELECT book_id, author_id FROM book_authors WHERE book_id IN "
            + "<foreach collection='bookIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>"
            + "</script>")
    List<BookAuthor> selectByBookIds(@Param("bookIds") List<Long> bookIds);

    @Select("SELECT author_id FROM book_authors WHERE book_id = #{bookId} ORDER BY author_id")
    List<Long> selectAuthorIds(@Param("bookId") Long bookId);

    @Select("SELECT COUNT(*) FROM book_authors WHERE book_id = #{bookId}")
    long countByBookId(@Param("bookId") Long bookId);

    @Delete("DELETE FROM book_authors WHERE book_id = #{bookId}")
    int deleteByBookId(@Param("bookId") Long bookId);

    @Insert("<script>INSERT INTO book_authors (book_id, author_id) VALUES "
            + "<foreach collection='authorIds' item='aid' separator=','>(#{bookId}, #{aid})</foreach>"
            + "</script>")
    int insertBatch(@Param("bookId") Long bookId, @Param("authorIds") List<Long> authorIds);
}
