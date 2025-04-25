package org.waitlight.simple.jsonql.jql;

public record Join(
        String type,    // JOIN类型：INNER/LEFT/RIGHT
        String table,   // 关联表名
        String on       // 关联条件
) {
}