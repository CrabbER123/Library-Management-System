package com.library.modules.reader.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 读者档案，与 users 一对一（user_id 唯一，可为空）。
 */
@Data
@TableName("readers")
public class Reader {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联登录账号；未开通在线账号的读者为 null。
     */
    private Long userId;

    private String cardNo;

    private String name;

    private String phone;

    private String email;

    /**
     * ACTIVE / SUSPENDED
     */
    private String status;

    private Integer maxBorrow;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
