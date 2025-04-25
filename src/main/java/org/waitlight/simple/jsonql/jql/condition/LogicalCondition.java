package org.waitlight.simple.jsonql.jql.condition;

import java.util.List;

public class LogicalCondition extends Condition {
    private String operator;
    private List<Condition> conditions;

    public LogicalCondition() {
        setType("logical");
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public List<Condition> getConditions() {
        return conditions;
    }

    public void setConditions(List<Condition> conditions) {
        this.conditions = conditions;
    }

    @Override
    public String toSql() {
        if (conditions == null || conditions.isEmpty()) {
            return "";
        }
        return "(" + String.join(" " + operator + " ", 
            conditions.stream().map(Condition::toSql).toList()) + ")";
    }
} 