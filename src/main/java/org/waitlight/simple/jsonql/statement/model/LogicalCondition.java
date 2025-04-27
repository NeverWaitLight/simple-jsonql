package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class LogicalCondition implements WhereCondition {
    private OperatorType operator;
    private List<WhereCondition> conditions;

    @Override
    public ConditionType getType() {
        return ConditionType.LOGICAL;
    }
}
