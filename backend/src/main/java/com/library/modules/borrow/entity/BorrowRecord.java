package com.library.modules.borrow.entity;

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
 * 借阅记录。active_flag 是数据库生成列（仅 BORROWED 为 1），用于保证同一读者同一本书只有一条在借记录，
 * 因此不映射为实体字段，避免插入时报错。
 */
@Data
@TableName("borrow_records")
public class BorrowRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long readerId;

    private Long bookId;

    private LocalDate borrowDate;

    private LocalDate dueDate;

    private LocalDate returnDate;

    private Integer renewTimes;

    /**
     * BORROWED / RETURNED。逾期不落独立状态，由 due_date 与 return_date 计算。
     */
    private String status;

    /**
     * 归还时结算的罚款金额（未逾期为 0.00）。
     */
    private BigDecimal fineAmount;

    private Long operatorId;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
