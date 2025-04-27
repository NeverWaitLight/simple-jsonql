package org.waitlight.simple.jsonql.statement.model;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class LogicalCondition implements WhereCondition {
    private String operator;
    private List<WhereCondition> conditions;

    @Override
    public String getType() {
        return "logical";
    }
} 