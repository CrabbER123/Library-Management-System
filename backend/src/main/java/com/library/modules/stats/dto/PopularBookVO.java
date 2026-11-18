package com.library.modules.stats.dto;

/** 借阅排行条目 */
public record PopularBookVO(long bookId, String title, String isbn, long borrowCount) {
}
