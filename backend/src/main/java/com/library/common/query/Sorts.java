package com.library.common.query;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import java.util.Locale;
import java.util.Map;

/**
 * 排序白名单工具：只允许前端按预定义字段排序，避免 SQL 注入与排序字段暴露。
 */
public final class Sorts {

    private Sorts() {
    }

    /**
     * @param sort           形如 "createdAt,desc" 或 "createdAt"（默认降序）
     * @param allowedColumns API 字段名 -> 数据库列名
     */
    public static String apply(QueryWrapper<?> wrapper, String sort, Map<String, String> allowedColumns) {
        if (sort == null || sort.isBlank() || allowedColumns.isEmpty()) {
            return null;
        }
        String[] parts = sort.split(",", 2);
        String column = allowedColumns.get(parts[0].trim());
        if (column == null) {
            return null;
        }
        boolean asc = parts.length > 1 && isAsc(parts[1].trim());
        wrapper.orderBy(true, asc, column);
        return column;
    }

    public static void applyDefault(QueryWrapper<?> wrapper, String column, boolean asc) {
        wrapper.orderBy(true, asc, column);
    }

    private static boolean isAsc(String direction) {
        String d = direction.toLowerCase(Locale.ROOT);
        return d.equals("asc") || d.equals("ascend") || d.equals("ascending");
    }
}
