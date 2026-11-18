package com.library.modules.book.dto;

/**
 * 作者简要信息，用于图书列表/详情展示，避免前端二次查询。
 */
public record AuthorBrief(Long id, String name) {
}
