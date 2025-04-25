package org.waitlight.simple.jsonql;

import java.util.List;

public record JsonQL(
        String statement,
        List<String> select,
        String from,
        Join join,      // 新增JOIN条件
        Where where     // 新增WHERE条件
) {
    public enum statement {
        SELECT,
        INSERT,
        UPDATE,
        DELETE;

    }

    public record Join(
            String type,    // JOIN类型：INNER/LEFT/RIGHT
            String table,   // 关联表名
            String on       // 关联条件
    ) {
    }

    public record Where(
            String condition // WHERE条件表达式
    ) {
    }
}