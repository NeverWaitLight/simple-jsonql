package org.waitlight.simple.jsonql.statement.model;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("operator")
    private MethodType operatorType;

    private Object value;
    private boolean not;

    @Override
    public ConditionType getType() {
        return ConditionType.COMPARISON;
    }
}
