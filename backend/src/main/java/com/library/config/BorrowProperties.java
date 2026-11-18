package com.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

/**
 * 借阅规则配置（application.yml 中 library.borrow.*）。<br>
 * 集中配置便于后续调整业务规则而无需改代码，阶段 4 的借还书逻辑将直接使用这些值。
 *
 * @param loanDays             默认借期（天）
 * @param maxRenewTimes        单次借阅最多续借次数
 * @param renewDays            每次续借延长天数
 * @param maxBorrowPerReader   每位读者默认最大同时借阅数（读者档案可单独覆盖）
 * @param finePerDay           逾期罚款单价（元/天）
 * @param allowRenewWhenOverdue 逾期后是否允许续借
 * @param maxFineAmount        单笔罚款上限（0 表示不封顶）
 */
@ConfigurationProperties(prefix = "library.borrow")
public record BorrowProperties(
        int loanDays,
        int maxRenewTimes,
        int renewDays,
        int maxBorrowPerReader,
        BigDecimal finePerDay,
        boolean allowRenewWhenOverdue,
        BigDecimal maxFineAmount) {
}
