package com.library.modules.book.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 图书。MVP 用 total_stock / available_stock 表示库存，不单独建副本表。
 */
@Data
@TableName("books")
public class Book {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String isbn;

    private String title;

    private String subtitle;

    private Long publisherId;

    private Long categoryId;

    private LocalDate publishDate;

    private String language;

    private BigDecimal price;

    private String coverUrl;

    private String summary;

    /**
     * 总库存（含借出）。
     */
    private Integer totalStock;

    /**
     * 可借库存，借出 -1、归还 +1，由数据库 CHECK 约束保证不为负且不超过总库存。
     */
    private Integer availableStock;

    /**
     * AVAILABLE / OFFLINE
     */
    private String status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
