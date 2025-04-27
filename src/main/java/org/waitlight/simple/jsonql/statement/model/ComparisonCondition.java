package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

/**
 * 比较条件类
 * 用于表示字段与值的比较操作
 */
@Getter
@Setter
public class ComparisonCondition implements WhereCondition {
    private String field;
    private String operator;
    private Object value;
    private boolean not;

    @Override
    public String getType() {
        return "comparison";
    }
} 