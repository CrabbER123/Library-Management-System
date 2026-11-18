package com.library.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 用户-角色关联（复合主键 user_id + role_id，此处以 user_id 作为 MyBatis-Plus 主键语义使用）。
 */
@Data
@TableName("user_roles")
public class UserRole {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    private Long roleId;
}
