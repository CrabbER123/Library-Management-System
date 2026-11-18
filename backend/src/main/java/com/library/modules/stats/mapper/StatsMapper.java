package com.library.modules.stats.mapper;

import com.library.modules.stats.dto.CategoryDistributionRow;
import com.library.modules.stats.dto.PopularBookRow;
import com.library.modules.stats.dto.StatsOverviewRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 统计查询：只读聚合，避免把统计逻辑散落进各业务 Service。
 * 聚合列统一 COALESCE，空库时返回 0 而不是 null。
 */
@Mapper
public interface StatsMapper {

    @Select("""
            SELECT
                (SELECT COUNT(*) FROM books)                                        AS total_titles,
                (SELECT COALESCE(SUM(total_stock), 0) FROM books)                   AS total_books,
                (SELECT COALESCE(SUM(available_stock), 0) FROM books)               AS available_stock,
                (SELECT COALESCE(SUM(total_stock - available_stock), 0) FROM books) AS borrowed_count,
                (SELECT COUNT(*) FROM borrow_records
                  WHERE status = 'BORROWED' AND due_date < CURDATE())               AS overdue_count,
                (SELECT COUNT(*) FROM readers)                                      AS total_readers,
                (SELECT COUNT(*) FROM readers WHERE status = 'ACTIVE')              AS active_readers,
                (SELECT COUNT(*) FROM borrow_records)                               AS total_borrows
            """)
    StatsOverviewRow selectOverview();

    @Select("""
            SELECT b.id AS book_id, b.title, b.isbn, COUNT(br.id) AS borrow_count
            FROM books b
            JOIN borrow_records br ON br.book_id = b.id
            GROUP BY b.id, b.title, b.isbn
            ORDER BY borrow_count DESC, b.id ASC
            LIMIT #{limit}
            """)
    List<PopularBookRow> selectPopularBooks(@Param("limit") int limit);

    @Select("""
            SELECT c.name AS category_name, COUNT(b.id) AS book_count
            FROM categories c
            LEFT JOIN books b ON b.category_id = c.id
            GROUP BY c.id, c.name
            ORDER BY book_count DESC, c.name ASC
            """)
    List<CategoryDistributionRow> selectCategoryDistribution();
}
