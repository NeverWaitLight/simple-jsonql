package org.waitlight.simple.jsonql.statement.model;

import org.waitlight.simple.jsonql.statement.QueryStatement;

import lombok.Getter;
import lombok.Setter;

/**
 * 子查询条件类
 * 用于表示 EXISTS 等子查询条件
 */
@Getter
@Setter
public class SubqueryCondition implements WhereCondition {
    private QueryStatement subquery;
    private boolean not;

    @Override
    public ConditionType getType() {
        return ConditionType.SUBQUERY;
    }
} 