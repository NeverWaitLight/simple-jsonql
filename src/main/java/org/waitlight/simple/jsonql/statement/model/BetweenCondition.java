package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

/**
 * BETWEEN 条件类
 * 用于表示字段在某个范围内的查询条件
 */
@Getter
@Setter
public class BetweenCondition implements WhereCondition {
    private String field;
    private Object start;
    private Object end;
    private boolean not;

    @Override
    public ConditionType getType() {
        return ConditionType.BETWEEN;
    }
}
