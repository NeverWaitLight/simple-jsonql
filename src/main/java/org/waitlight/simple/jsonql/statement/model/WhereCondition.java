package org.waitlight.simple.jsonql.statement.model;

/**
 * WHERE 条件接口
 * 所有条件类型都必须实现此接口
 */
public interface WhereCondition {
    /**
     * 获取条件类型
     * @return 条件类型：comparison, logical, subquery
     */
    String getType();
} 