package com.library.modules.category.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 图书分类。MVP 为单层结构（parent_id 预留，不参与业务）。
 */
@Data
@TableName("categories")
public class Category {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String code;

    private Long parentId;

    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
