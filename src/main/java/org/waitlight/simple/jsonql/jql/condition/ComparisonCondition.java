package org.waitlight.simple.jsonql.jql.condition;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ComparisonCondition extends Condition {
    private String field;
    private String operator;
    private Object value;

    public ComparisonCondition() {
        setType("comparison");
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toSql() {
        String valueStr = value instanceof String ? "'" + value + "'" : String.valueOf(value);
        return field + " " + operator + " " + valueStr;
    }
} 